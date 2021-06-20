package com.nepheletech.jred.runtime.nodes;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.dataformat.csv.CsvDataFormat;
import org.apache.commons.csv.CSVFormat;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonPrimitive;

public class CsvNode extends AbstractNode {

  private static final String CSV_NODE_OPERATION = "CSV_NODE_OPERATION";

  private final char sep;
  private final boolean hdrin;
  private final int skip;
  private final boolean parseStrings;
  private final boolean include_empty_strings;
  private final boolean include_null_values;
  private final String multi;
  private final String hdrout;
  private final String ret;

  private List<String> temp;

  private final CsvDataFormat unmarshalCsv = new CsvDataFormat();
  private final CsvDataFormat marshalCsv = new CsvDataFormat();

  public CsvNode(Flow flow, JtonObject config) {
    super(flow, config);

    this.sep = config.getAsCharacter("sep", ',');
    this.hdrin = config.getAsBoolean("hdrin", false);
    this.skip = config.getAsInt("skip", 0);
    this.parseStrings = config.getAsBoolean("strings", true);
    this.include_empty_strings = config.getAsBoolean("include_empty_strings", false);
    this.include_null_values = config.getAsBoolean("include_null_values", false);
    this.multi = config.getAsString("multi", "one");
    this.hdrout = config.getAsString("hdrout", "none"); // none, all, once
    this.ret = config.getAsString("ret", "\n").replace("\\n", "\n").replace("\\r", "\r");
    ;

    final String temp = config.getAsString("temp", null);

    if (temp != null) {
      this.temp = asList(temp.split(","));
    } else {
      this.temp = null;
    }

    // ---

    marshalCsv.setFormat(CSVFormat.DEFAULT);
    marshalCsv.setDelimiter(sep);
    marshalCsv.setRecordSeparator(ret);
    marshalCsv.setHeaderDisabled("none".equals(hdrout));
    marshalCsv.setQuoteDisabled(false);
    marshalCsv.setEscapeDisabled(true);

    unmarshalCsv.setFormat(CSVFormat.DEFAULT);
    unmarshalCsv.setDelimiter(sep);
    unmarshalCsv.setSkipHeaderRecord(hdrin);
  }

  @Override
  public void configure() throws Exception {
    super.configure();

    fromF("direct:%s#csv", getId())
        .toF("log:%s?level=TRACE&showAll=true", logger.getName())
        .choice().when(header(CSV_NODE_OPERATION).isEqualTo("unmarshal"))
        .toF("direct:%s#unmarshal", getId())
        .otherwise()
        .toF("direct:%s#marshal", getId());

    fromF("direct:%s#marshal", getId())
        .toF("log:%s?level=TRACE&showAll=true", logger.getName())
        .log("------------------------ MARSHAL ---[" + body())
        .marshal(marshalCsv)
        .toF("log:%s?level=TRACE&showAll=true", logger.getName())
        .process(this::doHandleStrData);

    if ("mult".equals(multi)) {

      fromF("direct:%s#unmarshal", getId())
          .toF("log:%s?level=TRACE&showAll=true", logger.getName())
          .log("------------------------ UN MARSHAL ---[" + body())
          .unmarshal(unmarshalCsv)
          .toF("log:%s?level=TRACE&showAll=true", logger.getName())
          .process(this::doHandleCsvData);

    } else {

      fromF("direct:%s#unmarshal", getId())
          .toF("log:%s?level=TRACE&showAll=true", logger.getName())
          .log("------------------------ UN MARSHAL ---[" + body())
          .unmarshal(unmarshalCsv).split(body())
          .toF("log:%s?level=INFO&showAll=true", logger.getName())
          .process(this::doHandleCsvData);
    }
  }

  protected String getAdditionalRoute() {
    return format("direct:%s#csv", getId());
  }

  @Override
  protected void onMessage(Exchange exchange, JtonObject msg) {
    logger.trace(">>> onMessage: exchange={}", exchange);

    final Message message = exchange.getIn();

    final JtonElement payload = msg.get("payload");
    if (payload.isJtonPrimitive() && payload.asJtonPrimitive().isString()) {
      // CSV ->
      message.setHeader(CSV_NODE_OPERATION, "unmarshal");
      message.setBody(payload.asString());
    } else {
      // -> CSV
      exchange.getIn().setHeader(CSV_NODE_OPERATION, "marshal");

      if (payload.isJtonObject()) {
        final Map<String, Object> body = new LinkedHashMap<>();
        for (Map.Entry<String, JtonElement> entry : payload.asJtonObject().entrySet()) {
          final JtonElement value = entry.getValue();
          if (value.isJtonPrimitive() && !value.asJtonPrimitive().isJtonTransient()) {
            body.put(entry.getKey(), value.asJtonPrimitive().getValue());
          }
        }
        message.setBody(body);
      } else if (payload.isJtonArray()) {
        final List<Map<String, Object>> body = new ArrayList<>();
        for (JtonElement e : payload.asJtonArray()) {
          final Map<String, Object> row = new LinkedHashMap<>();
          body.add(row);

          if (e.isJtonObject()) {
            for (Map.Entry<String, JtonElement> entry : e.asJtonObject().entrySet()) {
              final JtonElement value = entry.getValue();
              if (value.isJtonPrimitive() && !value.asJtonPrimitive().isJtonTransient()) {
                row.put(entry.getKey(), value.asJtonPrimitive().getValue());
              }
            }
          }
        }
        message.setBody(body);
      }
    }

    // save `msg'
    message.setHeader(CsvNode.class.getName(), msg);
  }

  private void doHandleCsvData(Exchange exchange) {
    logger.trace(">>> doHandleCsvData: exchange={}\n{}", exchange.getProperties(),
        exchange.getIn().getHeaders());

    final Message message = exchange.getIn();

    // restore `msg'
    final JtonObject msg = (JtonObject) message
        .removeHeader(CsvNode.class.getName());

    if (!"mult".equals(multi)) {

      @SuppressWarnings("unchecked")
      final List<String> csvData = message.getBody(List.class);

      final JtonObject payload = new JtonObject();

      if (csvData != null) {

        int startRow = skip;

        if (temp == null && hdrin) {
          this.temp = new ArrayList<>(csvData); // FIXME
          ++startRow;
        }

        for (int col = 0, m = csvData.size(); col < m; col++) {
          payload.set(getKey(col), doParseStrings(csvData.get(col)));
        }
      }

      msg.set("payload", payload).set("parts", new JtonObject()
          .set("id", msg.get("_msgid"))
          .set("index", exchange.getProperty(Exchange.SPLIT_INDEX, Integer.class))
          .set("count", exchange.getProperty(Exchange.SPLIT_SIZE, Integer.class))
          .set("complete", exchange.getProperty(Exchange.SPLIT_COMPLETE, Boolean.class)));

    } else {

      @SuppressWarnings("unchecked")
      final List<List<String>> csvData = message.getBody(List.class);

      final JtonArray payload = new JtonArray();

      if (csvData != null) {

        int startRow = skip;

        if (temp == null && hdrin) {
          this.temp = new ArrayList<>(csvData.get(0)); // FIXME
          ++startRow;
        }

        for (int row = startRow, n = csvData.size(); row < n; row++) {
          final List<String> csvRow = csvData.get(row);
          final JtonObject entry = new JtonObject();
          for (int col = 0, m = csvRow.size(); col < m; col++) {
            entry.set(getKey(col), doParseStrings(csvRow.get(col)));
          }
          payload.push(entry);
        }
      }

      msg.set("payload", payload);

    }

    send(exchange, msg);

//    } else {
//      for (int i = 0, n = payload.size(); i < n; i++) {
//        final JtonElement e =payload.get(i);
//        msg.set("payload", e);
//        
//        msg.set("parts", new JtonObject()
//            .set("id", msg.get("_msgid"))
//            .set("index", i)
//            .set("count", n));
//        
//        send(exchange, msg.deepCopy());
//      }
//    }
  }

  private void doHandleStrData(Exchange exchange) {
    logger.trace(">>> doHandleJtonData: exchange={}", exchange);

    final Message message = exchange.getIn();

    final String strData = message.getBody(String.class);
    logger.trace(">>> doHandleJtonData: strData={}", strData);

    // restore `msg'
    final JtonObject msg = (JtonObject) message
        .removeHeader(CsvNode.class.getName());
    msg.set("payload", strData);

    send(exchange, msg);
  }

  private JtonPrimitive doParseStrings(String value) {
    if (parseStrings) {
      try {
        return new JtonPrimitive(new BigDecimal(value));
      } catch (NumberFormatException e) {
        // ignore
      }
    }

    return new JtonPrimitive(value);
  }

  private String getKey(int index) {
    return temp != null ? temp.get(index) : "col" + index;
  }

}
