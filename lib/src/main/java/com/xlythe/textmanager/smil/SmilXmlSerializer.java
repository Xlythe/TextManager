package com.xlythe.textmanager.smil;

import android.util.Log;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

public class SmilXmlSerializer {
    private static final String TAG = "SmilXmlSerializer";

    public static void serialize(SMILDocument smilDoc, OutputStream out) {
        try {
            Writer writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"), 2048);

            writeElement(writer, smilDoc.getDocumentElement());
            writer.flush();
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "exception thrown", e);
        } catch (IOException e) {
            Log.e(TAG, "exception thrown", e);
        }
    }

    private static void writeElement(Writer writer, Element element)
            throws IOException {
        writer.write('<');
        writer.write(element.getTagName());

        if (element.hasAttributes()) {
            NamedNodeMap attributes = element.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Attr attribute = (Attr)attributes.item(i);
                writer.write(" " + attribute.getName());
                writer.write("=\"" + attribute.getValue() + "\"");
            }
        }

        // FIXME: Might throw ClassCastException
        SMILElement childElement = (SMILElement) element.getFirstChild();

        if (childElement != null) {
            writer.write('>');

            do {
                writeElement(writer, childElement);
                childElement = (SMILElement) childElement.getNextSibling();
            } while (childElement != null);

            writer.write("</");
            writer.write(element.getTagName());
            writer.write('>');
        } else {
            writer.write("/>");
        }
    }
}
