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
package org.nuxeo.ecm.webapp.diff.helpers;

import static org.jboss.seam.ScopeType.APPLICATION;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.Binary;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLBlob;
import org.nuxeo.ecm.platform.diff.diff_match_patch;
import org.nuxeo.ecm.platform.diff.helpers.ComplexPropertyHelper;
import org.nuxeo.ecm.platform.diff.model.PropertyDiff;
import org.nuxeo.ecm.platform.diff.model.impl.ListPropertyDiff;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * Helps handling property diff display.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
@Name("propertyDiffDisplayHelper")
@Scope(APPLICATION)
public class PropertyDiffDisplayHelperBean implements Serializable {

    private static final long serialVersionUID = -7995476720750309928L;

    @In(create = true)
    protected transient ResourcesAccessor resourcesAccessor;

    protected diff_match_patch dmp;

    @Create
    public void init() {
        dmp = new diff_match_patch();
    }

    public static Serializable getSimplePropertyValue(DocumentModel doc,
            String schemaName, String fieldName) throws ClientException {

        return ComplexPropertyHelper.getSimplePropertyValue(doc, schemaName,
                fieldName);
    }

    public List<String> getComplexItemNames(String schemaName, String fieldName)
            throws Exception {

        return ComplexPropertyHelper.getComplexItemNames(schemaName, fieldName);
    }

    public Serializable getComplexItemValue(DocumentModel doc,
            String schemaName, String fieldName, String complexItemName)
            throws ClientException {

        return ComplexPropertyHelper.getComplexItemValue(doc, schemaName,
                fieldName, complexItemName);
    }

    public List<Integer> getListItemIndexes(ListPropertyDiff listPropertyDiff)
            throws ClientException {

        return ComplexPropertyHelper.getListItemIndexes(listPropertyDiff);
    }

    public Serializable getListItemValue(DocumentModel doc, String schemaName,
            String fieldName, int itemIndex) throws ClientException {

        return ComplexPropertyHelper.getListItemValue(doc, schemaName,
                fieldName, itemIndex);

    }

    public List<String> getComplexListItemNames(String schemaName,
            String fieldName) throws Exception {

        return ComplexPropertyHelper.getComplexListItemNames(schemaName,
                fieldName);
    }

    public Serializable getComplexListItemValue(DocumentModel doc,
            String schemaName, String fieldName, int itemIndex,
            String complexItemName) throws ClientException {

        return ComplexPropertyHelper.getComplexListItemValue(doc, schemaName,
                fieldName, itemIndex, complexItemName);
    }

    public boolean isSimpleProperty(Serializable prop) {

        return ComplexPropertyHelper.isSimpleProperty(prop);
    }

    public boolean isComplexProperty(Serializable prop) {

        return ComplexPropertyHelper.isComplexProperty(prop);
    }

    public boolean isContentProperty(Serializable prop) {

        return ComplexPropertyHelper.isContentProperty(prop);
    }

    public boolean isListProperty(Serializable prop) {

        return ComplexPropertyHelper.isListProperty(prop);
    }

    /**
     * Gets the property display.
     * 
     * @param propertyValue the property value
     * @param propertyDiff the property diff
     * @return the property display
     */
    public String getPropertyDisplay(Serializable propertyValue,
            PropertyDiff propertyDiff) {

        String propertyDisplay;

        // Boolean
        if (propertyValue instanceof Boolean) {
            propertyDisplay = resourcesAccessor.getMessages().get(
                    "property.boolean." + propertyValue);
        }
        // Date
        else if (propertyValue instanceof Date) {
            DateFormat sdf = new SimpleDateFormat("dd MMMM yyyy - hh:mm");
            propertyDisplay = sdf.format(propertyValue);
        }
        // Calendar
        else if (propertyValue instanceof Calendar) {
            DateFormat sdf = new SimpleDateFormat("dd MMMM yyyy - hh:mm");
            propertyDisplay = sdf.format(((Calendar) propertyValue).getTime());
        }
        // SQLBlob
        else if (propertyValue instanceof SQLBlob) {
            Binary binary = ((SQLBlob) propertyValue).getBinary();
            if (binary == null) {
                propertyDisplay = "Null binary";
            } else {
                propertyDisplay = "Digest: " + binary.getDigest();
            }
        } // Binary
        else if (propertyValue instanceof Binary) {
            propertyDisplay = "Digest: " + ((Binary) propertyValue).getDigest();
        }
        // Default: we consider property value is a String.
        // Works fine for PropertyType.STRING, PropertyType.INTEGER,
        // PropertyType.LONG, PropertyType.DOUBLE.
        else {
            propertyDisplay = propertyValue.toString();
            // String leftValue = propertyDiff.getLeftValue();
            // String rightValue = propertyDiff.getRightValue();
            // if (leftValue == null) {
            // leftValue = "";
            // }
            // if (rightValue == null) {
            // rightValue = "";
            // }
            // LinkedList<Diff> diffs = dmp.diff_main(leftValue, rightValue);
            // propertyDisplay = dmp.diff_prettyHtml(diffs);

        }
        // TODO: Directory!

        return propertyDisplay;
    }
}
