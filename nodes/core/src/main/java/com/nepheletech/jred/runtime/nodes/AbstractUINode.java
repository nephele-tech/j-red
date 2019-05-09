package com.nepheletech.jred.runtime.nodes;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;

public abstract class AbstractUINode extends AbstractNode {
  private static final Logger logger = LoggerFactory.getLogger(AbstractUINode.class);
  
  private final MustacheFactory mf;
  private Mustache mustache;

	public AbstractUINode(Flow flow, JtonObject config, String template) {
		super(flow, config);

    mf = new DefaultMustacheFactory();
    
		try (InputStream is = getClass().getClassLoader().getResourceAsStream(template)) {
		  mustache = mf.compile(new InputStreamReader(is), template);
		} catch(IOException e) {
		  mustache = null;
		  
		  // TODO log
		  
		  throw new RuntimeException(e);
		}
	}
	
	@Override
  protected void onMessage(JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);
    
    final StringWriter w = new StringWriter();
    mustache.execute(w, prepare(msg.deepCopy()));
    msg.set("payload", w.toString());
    
    send(msg);
	}

  protected Object prepare(final JtonObject msg) {
    // TODO

    return msg;
  }
}
