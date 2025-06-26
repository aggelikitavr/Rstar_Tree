import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.ArrayList;
import java.util.List;

public class OSMParser {

    private List<Record> records;

    public OSMParser() {
        records = new ArrayList<>();
    }

    public void parseToRecords(String filename) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();

            parser.parse(filename, new DefaultHandler() {
                long currentId;
                double lat, lon;
                String name = null;
                boolean inNode = false;

                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    if (qName.equals("node")) {
                        inNode = true;
                        currentId = Long.parseLong(attributes.getValue("id"));
                        lat = Double.parseDouble(attributes.getValue("lat"));
                        lon = Double.parseDouble(attributes.getValue("lon"));
                        name = null;
                    } else if (inNode && qName.equals("tag")) {
                        String key = attributes.getValue("k");
                        if (key.equals("name")) {
                            name = attributes.getValue("v");
                        }
                    }
                }

                public void endElement(String uri, String localName, String qName) {
                    if (qName.equals("node")) {
                        if (name != null) {
                            records.add(new Record(currentId, name, new double[]{lat, lon}));
                        }
                        inNode = false;
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Εμφάνιση των εγγραφών
        for (Record r : records) {
            System.out.println(r);
        }
    }

    public List<Record> getRecords() {
        return records;
    }
}
