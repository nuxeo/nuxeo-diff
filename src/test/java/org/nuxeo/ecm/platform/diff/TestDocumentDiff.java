/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     ataillefer
 */
package org.nuxeo.ecm.platform.diff;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.BackendType;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.diff.model.DocumentDiff;
import org.nuxeo.ecm.platform.diff.model.PropertyType;
import org.nuxeo.ecm.platform.diff.model.SchemaDiff;
import org.nuxeo.ecm.platform.diff.model.impl.ComplexPropertyDiff;
import org.nuxeo.ecm.platform.diff.model.impl.ListPropertyDiff;
import org.nuxeo.ecm.platform.diff.model.impl.SimplePropertyDiff;
import org.nuxeo.ecm.platform.diff.service.DocumentDiffService;
import org.nuxeo.ecm.platform.xmlexport.DocumentXMLExporter;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * Tests document diff using DocumentDiffService.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(repositoryName = "default", type = BackendType.H2, init = DocumentDiffRepositoryInit.class, user = "Administrator", cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.platform.diff", "org.nuxeo.platform.diff.test" })
public class TestDocumentDiff extends DiffTestCase {

    @Inject
    protected CoreSession session;

    @Inject
    protected DocumentDiffService docDiffService;

    /**
     * Tests doc diff.
     * 
     * @throws ClientException the client exception
     */
    @Test
    public void testDocDiff() throws ClientException {

        // Get left and right docs
        DocumentModel leftDoc = session.getDocument(new PathRef(
                DocumentDiffRepositoryInit.LEFT_DOC_PATH));
        DocumentModel rightDoc = session.getDocument(new PathRef(
                DocumentDiffRepositoryInit.RIGHT_DOC_PATH));

        // Create XML export temporary files
        createXMLExportTempFile(leftDoc);
        createXMLExportTempFile(rightDoc);

        // Do doc diff
        DocumentDiff docDiff = docDiffService.diff(session, leftDoc, rightDoc);
        assertEquals("Wrong schema count.", 5, docDiff.getSchemaCount());

        // ---------------------------
        // Check system elements
        // ---------------------------
        SchemaDiff schemaDiff = checkSchemaDiff(docDiff, "system", 2);

        // type
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("type"),
                PropertyType.UNDEFINED, "SampleType", "OtherSampleType");
        // path
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("path"),
                PropertyType.UNDEFINED, "leftDoc", "rightDoc");

        // ---------------------------
        // Check dublincore schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "dublincore", 6);

        // title => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("title"),
                PropertyType.STRING, "My first sample", "My second sample");
        // description => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("description"),
                PropertyType.STRING, "description", null);
        // created => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("created"),
                PropertyType.DATE, "2011-12-29T11:24:25Z",
                "2011-12-30T12:05:02Z");
        // creator => same
        checkIdenticalField(schemaDiff.getFieldDiff("creator"));
        // modified => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("created"),
                PropertyType.DATE, "2011-12-29T11:24:25Z",
                "2011-12-30T12:05:02Z");
        // lastContributor => same once trimmed
        checkIdenticalField(schemaDiff.getFieldDiff("lastContributor"));
        // contributors => different (update) / same / different (add)
        ListPropertyDiff expectedListFieldDiff = new ListPropertyDiff(
                PropertyType.SCALAR_LIST);
        expectedListFieldDiff.putDiff(0, new SimplePropertyDiff(
                PropertyType.STRING, "Administrator", "anotherAdministrator"));
        expectedListFieldDiff.putDiff(2, new SimplePropertyDiff(
                PropertyType.STRING, null, "jack"));
        checkListFieldDiff(schemaDiff.getFieldDiff("contributors"),
                expectedListFieldDiff);
        // subjects => same / different (remove)
        expectedListFieldDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedListFieldDiff.putDiff(1, new SimplePropertyDiff(
                PropertyType.STRING, "Architecture", null));
        checkListFieldDiff(schemaDiff.getFieldDiff("subjects"),
                expectedListFieldDiff);

        // ---------------------------
        // Check simpletypes schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "simpletypes", 4);

        // string => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("string"),
                PropertyType.STRING, "a string property",
                "a different string property");
        // textarea => same
        checkIdenticalField(schemaDiff.getFieldDiff("textarea"));
        // boolean => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("boolean"),
                PropertyType.BOOLEAN, String.valueOf(Boolean.TRUE), null);
        // integer => same
        checkIdenticalField(schemaDiff.getFieldDiff("integer"));
        // date => same
        checkIdenticalField(schemaDiff.getFieldDiff("date"));
        // htmlText => different
        checkSimpleFieldDiff(
                schemaDiff.getFieldDiff("htmlText"),
                PropertyType.STRING,
                "&lt;p&gt;html text with &lt;strong&gt;&lt;span style=\"text-decoration: underline;\"&gt;styles&lt;/span&gt;&lt;/strong&gt;&lt;/p&gt;\n&lt;ul&gt;\n&lt;li&gt;and&lt;/li&gt;\n&lt;li&gt;nice&lt;/li&gt;\n&lt;li&gt;bullets&lt;/li&gt;\n&lt;/ul&gt;\n&lt;p&gt;&amp;nbsp;&lt;/p&gt;",
                "&lt;p&gt;html  text modified with &lt;span style=\"text-decoration: underline;\"&gt;styles&lt;/span&gt;&lt;/p&gt;\n&lt;ul&gt;\n&lt;li&gt;and&lt;/li&gt;\n&lt;li&gt;nice&lt;/li&gt;\n&lt;li&gt;bullets&lt;/li&gt;\n&lt;/ul&gt;\n&lt;p&gt;&amp;nbsp;&lt;/p&gt;");
        // multivalued => different (remove) * 4
        expectedListFieldDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedListFieldDiff.putDiff(0, new SimplePropertyDiff(
                PropertyType.STRING, "monday", null));
        expectedListFieldDiff.putDiff(1, new SimplePropertyDiff(
                PropertyType.STRING, "tuesday", null));
        expectedListFieldDiff.putDiff(2, new SimplePropertyDiff(
                PropertyType.STRING, "wednesday", null));
        expectedListFieldDiff.putDiff(3, new SimplePropertyDiff(
                PropertyType.STRING, "thursday", null));
        checkListFieldDiff(schemaDiff.getFieldDiff("multivalued"),
                expectedListFieldDiff);

        // ---------------------------
        // Check complextypes schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "complextypes", 2);

        // complex => same / different (update) / different (remove) / different
        // (add)
        ComplexPropertyDiff expectedComplexFieldDiff = new ComplexPropertyDiff(
                PropertyType.COMPLEX);
        expectedComplexFieldDiff.putDiff(
                "booleanItem",
                new SimplePropertyDiff(PropertyType.BOOLEAN,
                        String.valueOf(Boolean.TRUE),
                        String.valueOf(Boolean.FALSE)));
        expectedComplexFieldDiff.putDiff("integerItem", new SimplePropertyDiff(
                PropertyType.LONG, "10", null));
        expectedComplexFieldDiff.putDiff("dateItem", new SimplePropertyDiff(
                PropertyType.DATE, null, "2011-12-29T23:00:00Z"));
        checkComplexFieldDiff(schemaDiff.getFieldDiff("complex"),
                expectedComplexFieldDiff);

        // complexList =>
        // item1: same / different (update) / different (remove) / different
        // (add)
        // item2: add
        expectedListFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);

        ComplexPropertyDiff item1ExpectedComplexFieldDiff = new ComplexPropertyDiff(
                PropertyType.COMPLEX);
        item1ExpectedComplexFieldDiff.putDiff(
                "booleanItem",
                new SimplePropertyDiff(PropertyType.BOOLEAN,
                        String.valueOf(Boolean.TRUE),
                        String.valueOf(Boolean.FALSE)));
        item1ExpectedComplexFieldDiff.putDiff("integerItem",
                new SimplePropertyDiff(PropertyType.LONG, "12", null));
        item1ExpectedComplexFieldDiff.putDiff("dateItem",
                new SimplePropertyDiff(PropertyType.DATE, null,
                        "2011-12-30T23:00:00Z"));

        ComplexPropertyDiff item2ExpectedComplexFieldDiff = new ComplexPropertyDiff(
                PropertyType.COMPLEX);
        item2ExpectedComplexFieldDiff.putDiff("stringItem",
                new SimplePropertyDiff(PropertyType.STRING, null,
                        "second element of a complex list"));
        item2ExpectedComplexFieldDiff.putDiff(
                "booleanItem",
                new SimplePropertyDiff(PropertyType.BOOLEAN, null,
                        String.valueOf(Boolean.FALSE)));
        item2ExpectedComplexFieldDiff.putDiff("integerItem",
                new SimplePropertyDiff(PropertyType.LONG, null, "20"));
        item2ExpectedComplexFieldDiff.putDiff("dateItem",
                new SimplePropertyDiff(PropertyType.DATE, null, ""));

        expectedListFieldDiff.putDiff(0, item1ExpectedComplexFieldDiff);
        expectedListFieldDiff.putDiff(1, item2ExpectedComplexFieldDiff);

        checkListFieldDiff(schemaDiff.getFieldDiff("complexList"),
                expectedListFieldDiff);

        // ---------------------------
        // Check listoflists schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "listoflists", 1);

        // listOfLists =>
        // item1: same
        // item2: different
        // item3: add
        expectedListFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);

        item1ExpectedComplexFieldDiff = new ComplexPropertyDiff(
                PropertyType.COMPLEX);
        item1ExpectedComplexFieldDiff.putDiff("stringItem",
                new SimplePropertyDiff(PropertyType.STRING, "second item",
                        "second item is different"));
        ListPropertyDiff expectedNestedListDiff = new ListPropertyDiff(
                PropertyType.SCALAR_LIST);
        expectedNestedListDiff.putDiff(1, new SimplePropertyDiff(
                PropertyType.STRING, "Thursday", "Friday"));
        expectedNestedListDiff.putDiff(2, new SimplePropertyDiff(
                PropertyType.STRING, null, "Saturday"));
        expectedNestedListDiff.putDiff(3, new SimplePropertyDiff(
                PropertyType.STRING, null, "July"));
        expectedNestedListDiff.putDiff(4, new SimplePropertyDiff(
                PropertyType.STRING, null, "August"));
        item1ExpectedComplexFieldDiff.putDiff("stringListItem",
                expectedNestedListDiff);

        item2ExpectedComplexFieldDiff = new ComplexPropertyDiff(
                PropertyType.COMPLEX);
        item2ExpectedComplexFieldDiff.putDiff("stringItem",
                new SimplePropertyDiff(PropertyType.STRING, null, "third item"));
        item2ExpectedComplexFieldDiff.putDiff("stringListItem",
                new ListPropertyDiff(PropertyType.SCALAR_LIST));

        expectedListFieldDiff.putDiff(1, item1ExpectedComplexFieldDiff);
        expectedListFieldDiff.putDiff(2, item2ExpectedComplexFieldDiff);

        checkListFieldDiff(schemaDiff.getFieldDiff("listOfLists"),
                expectedListFieldDiff);

    }

    /**
     * Tests inverse doc diff.
     * 
     * @throws ClientException the client exception
     */
    @Test
    public void testInverseDocDiff() throws ClientException {

        // Get left and right docs
        DocumentModel leftDoc = session.getDocument(new PathRef(
                DocumentDiffRepositoryInit.RIGHT_DOC_PATH));
        DocumentModel rightDoc = session.getDocument(new PathRef(
                DocumentDiffRepositoryInit.LEFT_DOC_PATH));

        // Create XML export temporary files
        createXMLExportTempFile(leftDoc);
        createXMLExportTempFile(rightDoc);

        // Do doc diff
        DocumentDiff docDiff = docDiffService.diff(session, leftDoc, rightDoc);
        assertEquals("Wrong schema count.", 5, docDiff.getSchemaCount());

        // ---------------------------
        // Check system elements
        // ---------------------------
        SchemaDiff schemaDiff = checkSchemaDiff(docDiff, "system", 2);

        // type
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("type"),
                PropertyType.UNDEFINED, "OtherSampleType", "SampleType");
        // path
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("path"),
                PropertyType.UNDEFINED, "rightDoc", "leftDoc");

        // ---------------------------
        // Check dublincore schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "dublincore", 6);

        // title => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("title"),
                PropertyType.STRING, "My second sample", "My first sample");
        // description => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("description"),
                PropertyType.STRING, null, "description");
        // created => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("created"),
                PropertyType.DATE, "2011-12-30T12:05:02Z",
                "2011-12-29T11:24:25Z");
        // creator => same
        checkIdenticalField(schemaDiff.getFieldDiff("creator"));
        // modified => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("created"),
                PropertyType.DATE, "2011-12-30T12:05:02Z",
                "2011-12-29T11:24:25Z");
        // lastContributor => same once trimmed
        checkIdenticalField(schemaDiff.getFieldDiff("lastContributor"));
        // contributors => different (update) / same / different (remove)
        ListPropertyDiff expectedListFieldDiff = new ListPropertyDiff(
                PropertyType.SCALAR_LIST);
        expectedListFieldDiff.putDiff(0, new SimplePropertyDiff(
                PropertyType.STRING, "anotherAdministrator", "Administrator"));
        expectedListFieldDiff.putDiff(2, new SimplePropertyDiff(
                PropertyType.STRING, "jack", null));
        checkListFieldDiff(schemaDiff.getFieldDiff("contributors"),
                expectedListFieldDiff);
        // subjects => same / different (add)
        expectedListFieldDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedListFieldDiff.putDiff(1, new SimplePropertyDiff(
                PropertyType.STRING, null, "Architecture"));
        checkListFieldDiff(schemaDiff.getFieldDiff("subjects"),
                expectedListFieldDiff);

        // ---------------------------
        // Check simpletypes schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "simpletypes", 4);

        // string => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("string"),
                PropertyType.STRING, "a different string property",
                "a string property");
        // textarea => same
        checkIdenticalField(schemaDiff.getFieldDiff("textarea"));
        // boolean => different
        checkSimpleFieldDiff(schemaDiff.getFieldDiff("boolean"),
                PropertyType.BOOLEAN, null, String.valueOf(Boolean.TRUE));
        // integer => same
        checkIdenticalField(schemaDiff.getFieldDiff("integer"));
        // date => same
        checkIdenticalField(schemaDiff.getFieldDiff("date"));
        // htmlText => different
        checkSimpleFieldDiff(
                schemaDiff.getFieldDiff("htmlText"),
                PropertyType.STRING,
                "&lt;p&gt;html  text modified with &lt;span style=\"text-decoration: underline;\"&gt;styles&lt;/span&gt;&lt;/p&gt;\n&lt;ul&gt;\n&lt;li&gt;and&lt;/li&gt;\n&lt;li&gt;nice&lt;/li&gt;\n&lt;li&gt;bullets&lt;/li&gt;\n&lt;/ul&gt;\n&lt;p&gt;&amp;nbsp;&lt;/p&gt;",
                "&lt;p&gt;html text with &lt;strong&gt;&lt;span style=\"text-decoration: underline;\"&gt;styles&lt;/span&gt;&lt;/strong&gt;&lt;/p&gt;\n&lt;ul&gt;\n&lt;li&gt;and&lt;/li&gt;\n&lt;li&gt;nice&lt;/li&gt;\n&lt;li&gt;bullets&lt;/li&gt;\n&lt;/ul&gt;\n&lt;p&gt;&amp;nbsp;&lt;/p&gt;");
        // multivalued => different (add) * 4
        expectedListFieldDiff = new ListPropertyDiff(PropertyType.SCALAR_LIST);
        expectedListFieldDiff.putDiff(0, new SimplePropertyDiff(
                PropertyType.STRING, null, "monday"));
        expectedListFieldDiff.putDiff(1, new SimplePropertyDiff(
                PropertyType.STRING, null, "tuesday"));
        expectedListFieldDiff.putDiff(2, new SimplePropertyDiff(
                PropertyType.STRING, null, "wednesday"));
        expectedListFieldDiff.putDiff(3, new SimplePropertyDiff(
                PropertyType.STRING, null, "thursday"));
        checkListFieldDiff(schemaDiff.getFieldDiff("multivalued"),
                expectedListFieldDiff);

        // ---------------------------
        // Check complextypes schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "complextypes", 2);

        // complex => same / different (update) / different (add) / different
        // (remove)
        ComplexPropertyDiff expectedComplexFieldDiff = new ComplexPropertyDiff(
                PropertyType.COMPLEX);
        expectedComplexFieldDiff.putDiff(
                "booleanItem",
                new SimplePropertyDiff(PropertyType.BOOLEAN,
                        String.valueOf(Boolean.FALSE),
                        String.valueOf(Boolean.TRUE)));
        expectedComplexFieldDiff.putDiff("integerItem", new SimplePropertyDiff(
                PropertyType.LONG, null, "10"));
        expectedComplexFieldDiff.putDiff("dateItem", new SimplePropertyDiff(
                PropertyType.DATE, "2011-12-29T23:00:00Z", null));
        checkComplexFieldDiff(schemaDiff.getFieldDiff("complex"),
                expectedComplexFieldDiff);

        // complexList =>
        // item1: same / different (update) / different (add) / different
        // (remove)
        // item2: remove
        expectedListFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);

        ComplexPropertyDiff item1ExpectedComplexFieldDiff = new ComplexPropertyDiff(
                PropertyType.COMPLEX);
        item1ExpectedComplexFieldDiff.putDiff(
                "booleanItem",
                new SimplePropertyDiff(PropertyType.BOOLEAN,
                        String.valueOf(Boolean.FALSE),
                        String.valueOf(Boolean.TRUE)));
        item1ExpectedComplexFieldDiff.putDiff("integerItem",
                new SimplePropertyDiff(PropertyType.LONG, null, "12"));
        item1ExpectedComplexFieldDiff.putDiff("dateItem",
                new SimplePropertyDiff(PropertyType.DATE,
                        "2011-12-30T23:00:00Z", null));

        ComplexPropertyDiff item2ExpectedComplexFieldDiff = new ComplexPropertyDiff(
                PropertyType.COMPLEX);
        item2ExpectedComplexFieldDiff.putDiff("stringItem",
                new SimplePropertyDiff(PropertyType.STRING,
                        "second element of a complex list", null));
        item2ExpectedComplexFieldDiff.putDiff(
                "booleanItem",
                new SimplePropertyDiff(PropertyType.BOOLEAN,
                        String.valueOf(Boolean.FALSE), null));
        item2ExpectedComplexFieldDiff.putDiff("integerItem",
                new SimplePropertyDiff(PropertyType.LONG, "20", null));
        item2ExpectedComplexFieldDiff.putDiff("dateItem",
                new SimplePropertyDiff(PropertyType.DATE, "", null));

        expectedListFieldDiff.putDiff(0, item1ExpectedComplexFieldDiff);
        expectedListFieldDiff.putDiff(1, item2ExpectedComplexFieldDiff);

        checkListFieldDiff(schemaDiff.getFieldDiff("complexList"),
                expectedListFieldDiff);

        // ---------------------------
        // Check listoflists schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "listoflists", 1);

        // listOfLists =>
        // item1: same
        // item2: different
        // item3: add
        expectedListFieldDiff = new ListPropertyDiff(PropertyType.COMPLEX_LIST);

        item1ExpectedComplexFieldDiff = new ComplexPropertyDiff(
                PropertyType.COMPLEX);
        item1ExpectedComplexFieldDiff.putDiff("stringItem",
                new SimplePropertyDiff(PropertyType.STRING,
                        "second item is different", "second item"));
        ListPropertyDiff expectedNestedListDiff = new ListPropertyDiff(
                PropertyType.SCALAR_LIST);
        expectedNestedListDiff.putDiff(1, new SimplePropertyDiff(
                PropertyType.STRING, "Friday", "Thursday"));
        expectedNestedListDiff.putDiff(2, new SimplePropertyDiff(
                PropertyType.STRING, "Saturday", null));
        expectedNestedListDiff.putDiff(3, new SimplePropertyDiff(
                PropertyType.STRING, "July", null));
        expectedNestedListDiff.putDiff(4, new SimplePropertyDiff(
                PropertyType.STRING, "August", null));
        item1ExpectedComplexFieldDiff.putDiff("stringListItem",
                expectedNestedListDiff);

        item2ExpectedComplexFieldDiff = new ComplexPropertyDiff(
                PropertyType.COMPLEX);
        item2ExpectedComplexFieldDiff.putDiff("stringItem",
                new SimplePropertyDiff(PropertyType.STRING, "third item", null));
        item2ExpectedComplexFieldDiff.putDiff("stringListItem",
                new ListPropertyDiff(PropertyType.SCALAR_LIST));

        expectedListFieldDiff.putDiff(1, item1ExpectedComplexFieldDiff);
        expectedListFieldDiff.putDiff(2, item2ExpectedComplexFieldDiff);

        checkListFieldDiff(schemaDiff.getFieldDiff("listOfLists"),
                expectedListFieldDiff);

    }

    /**
     * Creates an XML export temp file.
     * 
     * @param doc the doc
     * @throws ClientException the client exception
     */
    protected final void createXMLExportTempFile(DocumentModel doc)
            throws ClientException {

        DocumentXMLExporter docXMLExporter = docDiffService.getDocumentXMLExporter();
        byte[] xmlExportByteArray = docXMLExporter.exportXMLAsByteArray(doc,
                session);

        File tempDir = new File("target/classes");
        File tempFile;
        OutputStream fos = null;
        try {
            tempFile = File.createTempFile("export_" + doc.getName() + "_",
                    ".xml", tempDir);
            fos = new FileOutputStream(tempFile);
            fos.write(xmlExportByteArray);
        } catch (IOException ioe) {
            throw ClientException.wrap(ioe);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ioe) {
                    throw ClientException.wrap(ioe);
                }
            }
        }
    }

}
