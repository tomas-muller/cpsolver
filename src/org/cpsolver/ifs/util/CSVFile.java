package org.cpsolver.ifs.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Support for CSV (comma separated) text files.
 * 
 * @version IFS 1.3 (Iterative Forward Search)<br>
 *          Copyright (C) 2006 - 2014 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          <a href="http://muller.unitime.org">http://muller.unitime.org</a><br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 3 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */

public class CSVFile implements Serializable {
    private static final long serialVersionUID = 1L;
    HashMap<String, Integer> iHeaderMap = null;
    CSVLine iHeader = null;
    List<CSVLine> iLines = null;
    String iSeparator = ",";
    String iQuotationMark = "\"";

    public CSVFile() {
    }

    public CSVFile(File file) throws IOException {
        load(file);
    }

    public CSVFile(File file, String separator) throws IOException {
        setSeparator(separator);
        load(file);
    }

    public CSVFile(File file, String separator, String quotationMark) throws IOException {
        setSeparator(separator);
        setQuotationMark(quotationMark);
        load(file);
    }
    
    public CSVFile(Reader file) throws IOException {
        load(file);
    }

    public CSVFile(Reader file, String separator) throws IOException {
        setSeparator(separator);
        load(file);
    }

    public CSVFile(Reader file, String separator, String quotationMark) throws IOException {
        setSeparator(separator);
        setQuotationMark(quotationMark);
        load(file);
    }

    public void setSeparator(String separator) {
        iSeparator = separator;
    }

    public String getSeparator() {
        return iSeparator;
    }

    public void setQuotationMark(String quotationMark) {
        iQuotationMark = quotationMark;
    }

    public String getQuotationMark() {
        return iQuotationMark;
    }

    public void load(File file) throws IOException {
        load(new FileReader(file));
    }
    
    public void load(Reader file) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(file);
            iHeader = new CSVLine(reader.readLine()); // read header
            iHeaderMap = new HashMap<String, Integer>();
            iLines = new ArrayList<CSVLine>();
            int idx = 0;
            for (Iterator<CSVField> i = iHeader.fields(); i.hasNext(); idx++) {
                CSVField field = i.next();
                iHeaderMap.put(field.toString(), idx);
            }
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0)
                    continue;
                iLines.add(new CSVLine(line));
            }
        } finally {
            if (reader != null)
                reader.close();
        }
    }
    
    public void save(File file) throws IOException {
        save(new FileWriter(file));
    }

    public void save(Writer file) throws IOException {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file);
            if (iHeader != null)
                writer.println(iHeader.toString());

            if (iLines != null) {
                for (CSVLine line : iLines) {
                    writer.println(line.toString());
                }
            }

            writer.flush();
        } finally {
            if (writer != null)
                writer.close();
        }
    }

    public CSVLine getHeader() {
        return iHeader;
    }

    public void setHeader(CSVLine header) {
        iHeader = header;
    }

    public List<CSVLine> getLines() {
        return iLines;
    }

    public int size() {
        return iLines.size();
    }

    public boolean isEmpty() {
        return iLines.isEmpty();
    }

    public CSVLine getLine(int idx) {
        return iLines.get(idx);
    }

    public Iterator<CSVLine> lines() {
        return iLines.iterator();
    }

    public void addLine(CSVLine line) {
        if (iLines == null)
            iLines = new ArrayList<CSVLine>();
        iLines.add(line);
    }

    public void addLine(String line) {
        if (iLines == null)
            iLines = new ArrayList<CSVLine>();
        iLines.add(new CSVLine(line));
    }

    public List<CSVLine> filter(CSVFilter filter) {
        List<CSVLine> ret = new ArrayList<CSVLine>();
        for (CSVLine line : iLines) {
            if (filter.match(line))
                ret.add(line);
        }
        return ret;
    }

    public CSVLine addLine() {
        CSVLine line = new CSVLine();
        addLine(line);
        return line;
    }

    public CSVLine addLine(CSVField fields[]) {
        CSVLine line = new CSVLine(fields);
        addLine(line);
        return line;
    }

    public CSVLine addLine(Collection<CSVField> fields) {
        CSVLine line = new CSVLine(fields);
        addLine(line);
        return line;
    }

    public CSVLine setHeader(CSVField fields[]) {
        CSVLine header = new CSVLine(fields);
        setHeader(header);
        return header;
    }

    public CSVLine setHeader(Collection<CSVField> fields) {
        CSVLine header = new CSVLine(fields);
        setHeader(header);
        return header;
    }

    /** Representation of a line of a CSV file */
    public class CSVLine implements Serializable {
        private static final long serialVersionUID = 1L;
        List<CSVField> iFields = new ArrayList<CSVField>(iHeader == null ? 10 : iHeader.size());

        public CSVLine(String line) {
            int idx = 0;
            int newIdx = 0;
            int fromIdx = 0;
            while ((newIdx = line.indexOf(iSeparator, fromIdx)) >= 0) {
                String field = line.substring(idx, newIdx);
                if (iQuotationMark != null && field.startsWith(iQuotationMark) && (!field.endsWith(iQuotationMark) || field.length() == 1)) {
                    fromIdx = newIdx + iSeparator.length();
                    continue;
                }
                iFields.add(new CSVField(field, iQuotationMark));
                idx = newIdx + iSeparator.length();
                fromIdx = idx;
            }
            iFields.add(new CSVField(line.substring(idx), iQuotationMark));
        }

        public CSVLine() {
        }

        public CSVLine(CSVField fields[]) {
            for (int i = 0; i < fields.length; i++)
                iFields.add(fields[i]);
        }

        public CSVLine(Collection<CSVField> fields) {
            iFields.addAll(fields);
        }

        public List<CSVField> getFields() {
            return iFields;
        }

        public int size() {
            return iFields.size();
        }

        public boolean isEmpty() {
            return iFields.isEmpty();
        }

        public CSVField getField(int idx) {
            try {
                return iFields.get(idx);
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            } catch (IndexOutOfBoundsException e) {
                return null;
            }
        }

        public void setField(int idx, CSVField field) {
            iFields.set(idx, field);
        }

        public Iterator<CSVField> fields() {
            return iFields.iterator();
        }

        public CSVField getField(String name) {
            Integer idx = iHeaderMap.get(name);
            return (idx == null ? null : getField(idx.intValue()));
        }

        public void setField(String name, CSVField field) {
            Integer idx = iHeaderMap.get(name);
            if (idx != null)
                setField(idx.intValue(), field);
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            for (Iterator<CSVField> i = iFields.iterator(); i.hasNext();) {
                CSVField field = i.next();
                if (field != null)
                    sb.append((iQuotationMark == null ? "" : iQuotationMark) + field.toString()
                            + (iQuotationMark == null ? "" : iQuotationMark));
                if (i.hasNext())
                    sb.append(iSeparator);
            }
            return sb.toString();
        }

        public void debug(int offset, PrintWriter out) {
            int idx = 0;
            for (Iterator<CSVField> i = iFields.iterator(); i.hasNext();) {
                CSVField field = i.next();
                if (field == null || field.toString().length() == 0)
                    continue;
                for (int j = 0; j < offset; j++)
                    out.print(" ");
                out.println(iHeader.getField(idx) + "=" + (iQuotationMark == null ? "" : iQuotationMark) + field
                        + (iQuotationMark == null ? "" : iQuotationMark));
            }
        }
    }

    /** Representation of a field of a CSV file */
    public static class CSVField implements Serializable {
        private static final long serialVersionUID = 1L;
        String iField = null;

        public CSVField(String field, String quotationMark) {
            field = field.trim();
            if (quotationMark != null && field.startsWith(quotationMark) && field.endsWith(quotationMark))
                field = field.substring(1, field.length() - 1);
            iField = field;
        }

        public CSVField(Object field) {
            set(field == null ? "" : field.toString());
        }

        public CSVField(int field) {
            set(field);
        }

        public CSVField(boolean field) {
            set(field);
        }

        public CSVField(double field) {
            set(field);
        }

        public CSVField(long field) {
            set(field);
        }

        public CSVField(float field) {
            set(field);
        }

        public void set(Object value) {
            iField = (value == null ? "" : value.toString());
        }

        public void set(int value) {
            iField = String.valueOf(value);
        }

        public void set(boolean value) {
            iField = (value ? "1" : "0");
        }

        public void set(double value) {
            iField = String.valueOf(value);
        }

        public void set(long value) {
            iField = String.valueOf(value);
        }

        public void set(float value) {
            iField = String.valueOf(value);
        }

        @Override
        public String toString() {
            return (iField == null ? "" : iField);
        }

        public boolean isEmpty() {
            return (iField.length() == 0);
        }

        public int toInt() {
            return toInt(0);
        }

        public int toInt(int defaultValue) {
            try {
                return Integer.parseInt(iField);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        public long toLong() {
            return toLong(0);
        }

        public long toLong(long defaultValue) {
            try {
                return Long.parseLong(iField);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        public double toDouble() {
            return toDouble(0);
        }

        public double toDouble(double defaultValue) {
            try {
                return Double.parseDouble(iField);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        public Date toDate() {
            int month = Integer.parseInt(iField.substring(0, 2));
            int day = Integer.parseInt(iField.substring(3, 5));
            int year = Integer.parseInt(iField.substring(6, 8));
            Calendar c = Calendar.getInstance(Locale.US);
            c.set(year, month - 1, day, 0, 0, 0);
            return c.getTime();
        }

        public boolean toBoolean() {
            return "Y".equalsIgnoreCase(iField) || "on".equalsIgnoreCase(iField) || "true".equalsIgnoreCase(iField)
                    || "1".equalsIgnoreCase(iField);
        }
    }

    /** An interface for filtering lines of a CSV file */
    public static interface CSVFilter {
        public boolean match(CSVLine line);
    }

    public static CSVFilter eq(String name, String value) {
        return (new CSVFilter() {
            String n, v;

            @Override
            public boolean match(CSVLine line) {
                return line.getField(n).equals(v);
            }

            private CSVFilter set(String n, String v) {
                this.n = n;
                this.v = v;
                return this;
            }
        }).set(name, value);
    }

    public static CSVFilter and(CSVFilter first, CSVFilter second) {
        return (new CSVFilter() {
            CSVFilter a, b;

            @Override
            public boolean match(CSVLine line) {
                return a.match(line) && b.match(line);
            }

            private CSVFilter set(CSVFilter a, CSVFilter b) {
                this.a = a;
                this.b = b;
                return this;
            }
        }).set(first, second);
    }

    public static CSVFilter or(CSVFilter first, CSVFilter second) {
        return (new CSVFilter() {
            CSVFilter a, b;

            @Override
            public boolean match(CSVLine line) {
                return a.match(line) || b.match(line);
            }

            private CSVFilter set(CSVFilter a, CSVFilter b) {
                this.a = a;
                this.b = b;
                return this;
            }
        }).set(first, second);
    }

    public static CSVFilter not(CSVFilter filter) {
        return (new CSVFilter() {
            CSVFilter f;

            @Override
            public boolean match(CSVLine line) {
                return !f.match(line);
            }

            private CSVFilter set(CSVFilter f) {
                this.f = f;
                return this;
            }
        }).set(filter);
    }
}
