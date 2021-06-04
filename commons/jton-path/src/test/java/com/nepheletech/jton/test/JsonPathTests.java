package com.nepheletech.jton.test;

import org.junit.Assert;
import org.junit.Test;

import com.jayway.jsonpath.JsonPath;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonParser;
import com.nepheletech.jton.jsonpath.JtonPathConfiguration;

public class JsonPathTests {
  
  /**
   * Initialize JsonPath to use JTON objects.
   */
  static {
    JtonPathConfiguration.configure();
  }

  private JtonElement o = JtonParser.parse("{\n" +
      "    \"store\": {\n" +
      "        \"book\": [\n" +
      "            {\n" +
      "                \"category\": \"reference\",\n" +
      "                \"author\": \"Nigel Rees\",\n" +
      "                \"title\": \"Sayings of the Century\",\n" +
      "                \"price\": 8.95\n" +
      "            },\n" +
      "            {\n" +
      "                \"category\": \"fiction\",\n" +
      "                \"author\": \"Evelyn Waugh\",\n" +
      "                \"title\": \"Sword of Honour\",\n" +
      "                \"price\": 12.99\n" +
      "            },\n" +
      "            {\n" +
      "                \"category\": \"fiction\",\n" +
      "                \"author\": \"Herman Melville\",\n" +
      "                \"title\": \"Moby Dick\",\n" +
      "                \"isbn\": \"0-553-21311-3\",\n" +
      "                \"price\": 8.99\n" +
      "            },\n" +
      "            {\n" +
      "                \"category\": \"fiction\",\n" +
      "                \"author\": \"J. R. R. Tolkien\",\n" +
      "                \"title\": \"The Lord of the Rings\",\n" +
      "                \"isbn\": \"0-395-19395-8\",\n" +
      "                \"price\": 22.99\n" +
      "            }\n" +
      "        ],\n" +
      "        \"bicycle\": {\n" +
      "            \"color\": \"red\",\n" +
      "            \"price\": 19.95\n" +
      "        }\n" +
      "    },\n" +
      "    \"expensive\": 10\n" +
      "}");

  @Test
  public void rootElement() {
    JtonElement result = JsonPath.read(o, "$");
    Assert.assertEquals(result.toString(), o.toString());
  }

  @Test
  public void authorsOfAllBooks() {
    JtonElement result = JsonPath.read(o, "$.store.book[*].author");
    Assert.assertEquals(result.toString(),
        "[\"Nigel Rees\",\"Evelyn Waugh\",\"Herman Melville\",\"J. R. R. Tolkien\"]");
  }

  @Test
  public void allAuthors() {
    JtonElement result = JsonPath.read(o, "$..author");
    Assert.assertEquals(result.toString(),
        "[\"Nigel Rees\",\"Evelyn Waugh\",\"Herman Melville\",\"J. R. R. Tolkien\"]");
  }

  @Test
  public void allThings() {
    JtonElement result = JsonPath.read(o, "$.store.*");
    Assert.assertEquals(result.toString(),
        "[[{\"category\":\"reference\",\"author\":\"Nigel Rees\",\"title\":\"Sayings of the Century\",\"price\":8.95},{\"category\":\"fiction\",\"author\":\"Evelyn Waugh\",\"title\":\"Sword of Honour\",\"price\":12.99},{\"category\":\"fiction\",\"author\":\"Herman Melville\",\"title\":\"Moby Dick\",\"isbn\":\"0-553-21311-3\",\"price\":8.99},{\"category\":\"fiction\",\"author\":\"J. R. R. Tolkien\",\"title\":\"The Lord of the Rings\",\"isbn\":\"0-395-19395-8\",\"price\":22.99}],{\"color\":\"red\",\"price\":19.95}]");
  }

  @Test
  public void thePriceOfEveryting() {
    JtonElement result = JsonPath.read(o, "$.store..price");
    Assert.assertEquals(result.toString(), "[8.95,12.99,8.99,22.99,19.95]");
  }

  @Test
  public void theThirdBook() {
    JtonElement result = JsonPath.read(o, "$..book[2]");
    Assert.assertEquals(result.toString(), "[{\"category\":\"fiction\",\"author\":\"Herman Melville\",\"title\":\"Moby Dick\",\"isbn\":\"0-553-21311-3\",\"price\":8.99}]");
  }

  @Test
  public void theSecondToLast() {
    JtonElement result = JsonPath.read(o, "$..book[-2]");
    Assert.assertEquals(result.toString(), "[{\"category\":\"fiction\",\"author\":\"Herman Melville\",\"title\":\"Moby Dick\",\"isbn\":\"0-553-21311-3\",\"price\":8.99}]");
  }

  @Test
  public void theFirstTwoBooks() {
    JtonElement result = JsonPath.read(o, "$..book[0,1]");
    Assert.assertEquals(result.toString(), "[{\"category\":\"reference\",\"author\":\"Nigel Rees\",\"title\":\"Sayings of the Century\",\"price\":8.95},{\"category\":\"fiction\",\"author\":\"Evelyn Waugh\",\"title\":\"Sword of Honour\",\"price\":12.99}]");
  }
  
  @Test
  public void bookNo2FromTail() {
    JtonElement result = JsonPath.read(o, "$..book[2:]");
    Assert.assertEquals(result.toString(), "[{\"category\":\"fiction\",\"author\":\"Herman Melville\",\"title\":\"Moby Dick\",\"isbn\":\"0-553-21311-3\",\"price\":8.99},{\"category\":\"fiction\",\"author\":\"J. R. R. Tolkien\",\"title\":\"The Lord of the Rings\",\"isbn\":\"0-395-19395-8\",\"price\":22.99}]");
  }
  
  @Test
  public void allBooksWithISBNNo() {
    JtonElement result = JsonPath.read(o, "$..book[?(@.isbn)]");
    Assert.assertEquals(result.toString(), "[{\"category\":\"fiction\",\"author\":\"Herman Melville\",\"title\":\"Moby Dick\",\"isbn\":\"0-553-21311-3\",\"price\":8.99},{\"category\":\"fiction\",\"author\":\"J. R. R. Tolkien\",\"title\":\"The Lord of the Rings\",\"isbn\":\"0-395-19395-8\",\"price\":22.99}]");
  }
  
  @Test
  public void allBooksThatAreNotExpensive() {
    JtonElement result = JsonPath.read(o, "$..book[?(@.price <= $['expensive'])]");
    Assert.assertEquals(result.toString(), "[{\"category\":\"reference\",\"author\":\"Nigel Rees\",\"title\":\"Sayings of the Century\",\"price\":8.95},{\"category\":\"fiction\",\"author\":\"Herman Melville\",\"title\":\"Moby Dick\",\"isbn\":\"0-553-21311-3\",\"price\":8.99}]");
  }
  
  @Test
  public void allBooksMatchingRegex() {
    JtonElement result = JsonPath.read(o, "$..book[?(@.author =~ /.*REES/i)]");
    Assert.assertEquals(result.toString(), "[{\"category\":\"reference\",\"author\":\"Nigel Rees\",\"title\":\"Sayings of the Century\",\"price\":8.95}]");
  }
  
  @Test
  public void everyting() {
    JtonElement result = JsonPath.read(o, "$..*");
    Assert.assertEquals(result.toString(), result.toString());
  }
  
  @Test
  public void theNumberOfBooks() {
    JtonElement result = JsonPath.read(o, "$..book.length()");
    Assert.assertEquals(result.toString(), "[4]");
  }

}
