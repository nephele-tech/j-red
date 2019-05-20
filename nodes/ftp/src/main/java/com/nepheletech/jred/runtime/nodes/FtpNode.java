/*
 * Copyright NepheleTech and other contributorns, http://www.nephelerech.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nepheletech.jred.runtime.nodes;

import java.util.Map.Entry;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonPrimitive;

// ftp://[username@]hostname[:port]/directoryname[?options] 
// sftp://[username@]hostname[:port]/directoryname[?options] 
// ftps://[username@]hostname[:port]/directoryname[?options]
public class FtpNode extends AbstractCamelNode implements HasCredentials {
  private final boolean secure;

  private final String host;
  private final String port;
  private final String path;

  private String username;
  private String password;

  private String privateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
      "MIIEpAIBAAKCAQEAwp3ma51hBq5IUKVsPPtALnpzSI4lBxcl74d0B1YRVo5e03q6\n" +
      "vfBrrAxFdbfL2shq9SoC/WrtJ8dc34QUH+ba0S3SPm4QlEaKeG2K9nzPJ40lSEKf\n" +
      "nP/WZCfDmDmskNptZNz8KFrYAgOvn1eUlhCRSqc0YnRBH2OHtzHRFIz4hcHk0EKu\n" +
      "DAMZbjlk7botI3mdlNDUUFihZTGlXbl0uJpAFJv+oDTOAGlfwEj/iusVSvTHl3eR\n" +
      "y6Pzdy7GQwgFHTd9m1b0NJaBK5vz9Qbbg5FmUCPAbTAH5E/rct7sWDie80cKxe/s\n" +
      "YzcOb10mDhMq8SN6OPYGpERmHKAsvjtOWvCUYQIDAQABAoIBAF+jVO6hgmLUFrl6\n" +
      "8XvuRJYSmsCXCd84Iu5Y9E3+n7qXcEYg09Ava9o6lY/J6KX1Q4qDQMDkYPLhZ+R3\n" +
      "daRPwEx3ZhBD6mXTXIzd2CJVJz/afu3Jv8JZfzPszpPqE8GDdarRZiE2s1waD54Q\n" +
      "XfZ44c176aRQWQhh0vnVw1eEB0VDLQMiIHxq5sT7ctc/wfD0lbVPMjBkysCe0SIq\n" +
      "EEwvq9YE9Uy3eJXXxzDihvbRqtTrXONGTaBiXo6ltDh0UKyoxPlgXSTfvBA2stqM\n" +
      "bfHUNIiXukha9MYUC7dXowlIsT9pXQ7Ky0Po8FoK0S2KJHW0U48FkXKe5tcLyi9d\n" +
      "G77Q3zkCgYEA9qXH4R+x9tgiFLDrl0iUVDFM2ps/QWO35KpOZDsrKEJZ8GFaTv7d\n" +
      "tYVbsetbKq01LsKLm2BsOvvnHd9smEQ3KrpDzXvb+jysWm6aPUJxoi+QTGfCTMTB\n" +
      "ZMDmKCADSx4TMsmPqiJ35bdbJo5DhyL6qENK0U+XltH0iDo+EOcaMGcCgYEAyf8N\n" +
      "21qWhNnEAteZBEZHuD5zbhDODwcbVqfZ4iFjArpDHDzwjT1py3N2Fn7S6XeHZnqQ\n" +
      "3KCaHqHACzk8UQDMKpv5EzB+ngRkR7CfU+o13GmblfpaoP7E7fikWM7C5FNov1PM\n" +
      "ZOuMrpG7nAJFzqKDnhc8VZXqj36qrdKLkB70d/cCgYBVG4NLBTrNaGrqJNpGS38T\n" +
      "Rie0zxkWoSgVOUbvgxMzQyHxtkYJOBycxDWNwR6mLTpKtkqiBLWT7NQlFLFOIUu/\n" +
      "7KH0x6ZyRHlcgwmp1cGMK0lpc+wxvTrWX1GLyvAX/Xq2baKrHFWu6IQFW9WlTcac\n" +
      "RQwOCZ7PJgdw8lf0USpSRQKBgQDIBtwv9brL/+FMdzc9tmLbHmyr99Q1qXH3E+x9\n" +
      "M1aaotxGIEqNct1K7SFpEVnGe+kHICRHdnpoDwDfnGQQStvi7eVujT01OWAcZdjA\n" +
      "/lMr2yCJGguZFfDpPqlwj/8Kn1ZA8Licz2zWidXgnZeHEgOID7ZdgkFuq55U1wQB\n" +
      "ScYI7QKBgQCELwiCG1sXL5J7DBBRVX29yddW/CVHvkAFG2a1Vq+F7zxW86Swu1qk\n" +
      "oGTBMG4e1z6Eh9EyFd2cuZBSxwpJyxlLSqhiXeXzn/JYba/6F+iSnTtDuuIsplea\n" +
      "Dhom49G+CjG0jmU++jOBVpnYqveJ0n5deLWFws53YZM6T1/rg/UJ7g==\n" +
      "-----END RSA PRIVATE KEY-----";

  public FtpNode(Flow flow, JtonObject config) {
    super(flow, config);

    this.secure = config.getAsBoolean("secure", false);

    this.host = config.getAsString("host");
    this.port = config.getAsString("port", this.secure ? "22" : "21");
    this.path = config.getAsString("path", "");
  }

  public void setCredentials(JtonObject credentials) {
    if (credentials != null) {
      this.username = credentials.getAsString("username", null);
      this.password = credentials.getAsString("password", null);
    } else {
      this.username = null;
      this.password = null;
    }
  }

  @Override
  protected void addRoutes(CamelContext camelContext) throws Exception {
    final String ftpUrl = String.format("%sftp://%s@%s:%s%s?password=RAW(%s)&privateKey=%s&autoCreate=true&binary=true",
        (secure ? "s" : ""), username, host, port, path, password, "#privateKey");

    logger.info("====================={}", ftpUrl);

    camelContext.getRegistry()
        .bind("privateKey", privateKey.getBytes());

    camelContext.addRoutes(new RouteBuilder() {
      // onException(Exception.class)

      @Override
      public void configure() throws Exception {
        from("direct:" + getId())
            .to("log:DEBUG?showBody=true&showHeaders=true")
            .to(ftpUrl);
      }
    });
  }

  @Override
  protected void onMessage(JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    final ProducerTemplate template = getCamelContext().createProducerTemplate();
    template.send("direct:" + getId(), new Processor() {
      public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();

        in.setBody(msg.get("payload").toString(), String.class);

        final JtonObject headers = msg.getAsJtonObject("headers", false);
        if (headers != null) {
          for (Entry<String, JtonElement> entry : headers.entrySet()) {
            final String key = entry.getKey();
            final JtonElement value = entry.getValue();
            if (value.isJtonPrimitive()) {
              final JtonPrimitive _value = value.asJtonPrimitive();
              if (_value.isJtonTransient()) {
                in.getHeaders().put(key, _value.getValue());
              } else {
                in.getHeaders().put(key, _value);
              }
            }
          }
        }
      }
    });
  }
}
