/*
 * $Id$
 * ============================================================================
 *                    The Apache Software License, Version 1.1
 * ============================================================================
 *
 * Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment: "This product includes software
 *    developed by the Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "FOP" and "Apache Software Foundation" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache", nor may
 *    "Apache" appear in their name, without prior written permission of the
 *    Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ============================================================================
 *
 * The RTF library of the FOP project consists of voluntary contributions made by
 * many individuals on behalf of the Apache Software Foundation and was originally
 * created by Bertrand Delacretaz <bdelacretaz@codeconsult.ch> and contributors of
 * the jfor project (www.jfor.org), who agreed to donate jfor to the FOP project.
 * For more information on the Apache Software Foundation, please
 * see <http://www.apache.org/>.
 */

package org.apache.fop.rtf.rtflib.rtfdoc;

import java.io.*;
import java.util.*;

/**  Container for RtfTableCell elements
 *  @author Bertrand Delacretaz bdelacretaz@codeconsult.ch
 *  @author Andreas Putz a.putz@skynamics.com
 *  @author Roberto Marra roberto@link-u.com
 */

public class RtfTableRow extends RtfContainer implements ITableAttributes {
    private RtfTableCell m_cell;
    private RtfExtraRowSet m_extraRowSet;
    private int id;
    private int highestCell=0;


    /** Create an RTF element as a child of given container */
    RtfTableRow(RtfTable parent, Writer w, int idNum) throws IOException {
        super(parent,w);
        id = idNum;
    }

    /** Create an RTF element as a child of given container */
    RtfTableRow(RtfTable parent, Writer w, RtfAttributes attrs, int idNum) throws IOException {
        super(parent,w,attrs);
        id=idNum;
    }

    /** close current cell if any and start a new one */
    public RtfTableCell newTableCell(int cellWidth) throws IOException {
        highestCell++;
        m_cell = new RtfTableCell(this,m_writer,cellWidth,highestCell);
        return m_cell;
    }

    /** close current cell if any and start a new one */
    public RtfTableCell newTableCell(int cellWidth, RtfAttributes attrs) throws IOException {
        highestCell++;
        m_cell = new RtfTableCell(this,m_writer,cellWidth, attrs,highestCell);
        return m_cell;
    }

    /**
     * Added by Boris POUDEROUS on 07/02/2002
     * in order to add an empty cell that is merged with the cell above.
     * This cell is placed before or after the nested table.
     */
    public RtfTableCell newTableCellMergedVertically(int cellWidth, RtfAttributes attrs) throws IOException {
        highestCell++;
        m_cell = new RtfTableCell (this, m_writer, cellWidth, attrs, highestCell);
        m_cell.setVMerge(RtfTableCell.MERGE_WITH_PREVIOUS);
        return m_cell;
    }

    /**
     * Added by Boris POUDEROUS on 07/02/2002
     * in order to add an empty cell that is merged with the previous cell.
     */
    public RtfTableCell newTableCellMergedHorizontally (int cellWidth, RtfAttributes attrs) throws IOException {
        highestCell++;
        // Added by Normand Masse
        // Inherit attributes from base cell for merge
        RtfAttributes wAttributes = (RtfAttributes)attrs.clone();
        wAttributes.unset( "number-columns-spanned" );

        m_cell = new RtfTableCell(this,m_writer,cellWidth,wAttributes,highestCell);
        m_cell.setHMerge(RtfTableCell.MERGE_WITH_PREVIOUS);
        return m_cell;
    }

    protected void writeRtfPrefix() throws IOException {
        writeGroupMark(true);
    }

    /** overridden to write trowd and cell definitions before writing our cells */
    protected void writeRtfContent() throws IOException {

        // create new extra row set to allow our cells to put nested tables
        // in rows that will be rendered after this one
        m_extraRowSet = new RtfExtraRowSet(m_writer);

        // render the row and cells definitions
        writeControlWord("trowd");

        //check for keep-together
        if(m_attrib != null && m_attrib.isSet(ITableAttributes.ROW_KEEP_TOGETHER)) {
            writeControlWord(ROW_KEEP_TOGETHER);
        }

        writePaddingAttributes();

        // if we have attributes, manipulate border properties
        final RtfTable parentTable = (RtfTable) m_parent;
        if(m_attrib != null && parentTable != null) {

            //if table is only one row long
            if(isFirstRow() && parentTable.isHighestRow(id)){
                m_attrib.unset(ITableAttributes.ROW_BORDER_HORIZONTAL);
                //or if row is the first row
            }else if(isFirstRow()){
                m_attrib.unset(ITableAttributes.ROW_BORDER_BOTTOM);
                //or if row is the last row
            }else if(parentTable.isHighestRow(id)){
                m_attrib.unset(ITableAttributes.ROW_BORDER_TOP);
                //else the row is an inside row
            }else{
                m_attrib.unset(ITableAttributes.ROW_BORDER_BOTTOM);
                m_attrib.unset(ITableAttributes.ROW_BORDER_TOP);
            }
        }

        writeAttributes(m_attrib,ITableAttributes.ROW_BORDER);
        writeAttributes(m_attrib,ITableAttributes.CELL_BORDER);
        writeAttributes(m_attrib,BorderAttributesConverter.BORDERS);

        /**
         * Added by Boris POUDEROUS on 07/02/2002
         * in order to get the indexes of the cells preceding a cell that contains a nested table.
         * Thus, the cells of the extra row will be merged with the cells above.
         */
        boolean nestedTableFound = false;
        int index = 0; // Used to store the index of the cell that contains a nested table
        int numberOfCellsBeforeNestedTable = 0;

        java.util.Vector indexesFound = new java.util.Vector();
        for (Iterator it = getChildren().iterator(); it.hasNext(); )
          {
            final RtfElement e = (RtfElement)it.next();

            if (e instanceof RtfTableCell)
              {
                if (!nestedTableFound)
                  ++numberOfCellsBeforeNestedTable;

                for (Iterator it2 = ((RtfTableCell)e).getChildren().iterator(); it2.hasNext(); )
                   {
                      final RtfElement subElement = (RtfElement)it2.next();
                      if (subElement instanceof RtfTable)
                        {
                          nestedTableFound = true;
                          indexesFound.addElement(new Integer(index));
                        }
                      else if (subElement instanceof RtfParagraph)
                        {
                           for (Iterator it3 = ((RtfParagraph)subElement).getChildren().iterator(); it3.hasNext(); )
                             {
                                final RtfElement subSubElement = (RtfElement)it3.next();
                                if (subSubElement instanceof RtfTable)
                                  {
                                    nestedTableFound = true;
                                    indexesFound.addElement(new Integer(index));
                                  }
                             }
                        }
                   }
              }

            index++;
          }
         /** - end - */

        // write X positions of our cells
        int xPos = 0;
        index = 0;            // Line added by Boris POUDEROUS on 07/02/2002
        for(Iterator it = getChildren().iterator(); it.hasNext(); ) {
            final RtfElement e = (RtfElement)it.next();
            if(e instanceof RtfTableCell) {
                /**
                 * Added by Boris POUDEROUS on 2002/07/02
                 */
                // If one of the row's child cells contains a nested table :
                if (!indexesFound.isEmpty())
                  {
                    for (int i = 0; i < indexesFound.size(); i++)
                      {
                        // If the current cell index is equals to the index of the cell that
                        // contains a nested table => NO MERGE
                        if (index == ((Integer)indexesFound.get(i)).intValue())
                          break;

                        // If the current cell index is lower than the index of the cell that
                        // contains a nested table => START VERTICAL MERGE
                        else if (index < ((Integer)indexesFound.get(i)).intValue())
                          {
                            ((RtfTableCell)e).setVMerge(RtfTableCell.MERGE_START);
                            break;
                          }

                        // If the current cell index is greater than the index of the cell that
                        // contains a nested table => START VERTICAL MERGE
                        else if (index > ((Integer)indexesFound.get(i)).intValue())
                          {
                            ((RtfTableCell)e).setVMerge(RtfTableCell.MERGE_START);
                            break;
                          }
                      }
                  }
                /** - end - */

                // Added by Normand Masse
                // Adjust the cell's display attributes so the table's/row's borders
                // are drawn properly.
                RtfTableCell cell = (RtfTableCell)e;
                if ( index == 0 ) {
                    if ( !cell.getRtfAttributes().isSet( ITableAttributes.CELL_BORDER_LEFT ) ) {
                        cell.getRtfAttributes().set( ITableAttributes.CELL_BORDER_LEFT,
                            (String)m_attrib.getValue( ITableAttributes.ROW_BORDER_LEFT ));
                    }
                }

                if ( index == this.getChildCount() -1 ) {
                    if ( !cell.getRtfAttributes().isSet( ITableAttributes.CELL_BORDER_RIGHT ) ) {
                        cell.getRtfAttributes().set( ITableAttributes.CELL_BORDER_RIGHT,
                            (String)m_attrib.getValue( ITableAttributes.ROW_BORDER_RIGHT ));
                    }
                }

                if ( isFirstRow() ) {
                    if ( !cell.getRtfAttributes().isSet( ITableAttributes.CELL_BORDER_TOP ) ) {
                        cell.getRtfAttributes().set( ITableAttributes.CELL_BORDER_TOP,
                            (String)m_attrib.getValue( ITableAttributes.ROW_BORDER_TOP ));
                    }
                }

                if ( parentTable.isHighestRow(id) ) {
                    if ( !cell.getRtfAttributes().isSet( ITableAttributes.CELL_BORDER_BOTTOM ) ) {
                        cell.getRtfAttributes().set( ITableAttributes.CELL_BORDER_BOTTOM,
                            (String)m_attrib.getValue( ITableAttributes.ROW_BORDER_BOTTOM ));
                    }
                }

                xPos = cell.writeCellDef(xPos);
            }
          index++; // Added by Boris POUDEROUS on 2002/07/02
        }

        // now children can write themselves, we have the correct RTF prefix code
        super.writeRtfContent();
    }

    /** overridden to write RTF suffix code, what comes after our children */
    protected void writeRtfSuffix() throws IOException {
        writeControlWord("row");

        // write extra rows if any
        m_extraRowSet.writeRtf();
        writeGroupMark(false);
    }

    RtfExtraRowSet getExtraRowSet() {
        return m_extraRowSet;
    }

    private void writePaddingAttributes()
    throws IOException {
        // Row padding attributes generated in the converter package
        // use RTF 1.6 definitions - try to compute a reasonable RTF 1.5 value out of them if present
        // how to do vertical padding with RTF 1.5?
        if(m_attrib != null && !m_attrib.isSet(ATTR_RTF_15_TRGAPH)) {
            int gaph = -1;
            try {
                // set (RTF 1.5) gaph to the average of the (RTF 1.6) left and right padding values
                final Integer leftPadStr = (Integer)m_attrib.getValue(ATTR_ROW_PADDING_LEFT);
                if(leftPadStr != null) gaph = leftPadStr.intValue();
                final Integer rightPadStr = (Integer)m_attrib.getValue(ATTR_ROW_PADDING_RIGHT);
                if(rightPadStr != null) gaph = (gaph + rightPadStr.intValue()) / 2;
            } catch(Exception e) {
                final String msg = "RtfTableRow.writePaddingAttributes: " + e.toString();
//                getRtfFile().getLog().logWarning(msg);
            }
            if(gaph >= 0) {
                m_attrib.set(ATTR_RTF_15_TRGAPH,gaph);
            }
        }

        // write all padding attributes
        writeAttributes(m_attrib, ATTRIB_ROW_PADDING);
    }

    public boolean isFirstRow(){
        if(id == 1)
            return true;
        else
            return false;
    }

    public boolean isHighestCell(int id) {
        return (highestCell == id) ? true : false;
    }
}
