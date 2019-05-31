package org.cpsolver.teams;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;

public class RoomNearby extends Feature {
    Pattern iPattern = Pattern.compile("([A-Za-z]+)[- ]?([0-9]+)[a-zA-Z]?");
    
    public RoomNearby(String key, String[] fallbacks) {
        super(key, fallbacks);
    }
    
    public RoomNearby(String... keyWithFallbacks) {
        super(keyWithFallbacks);
    }
    
    @Override
    public double similar(Student a, Student b) {
        return diff(getProperty(a), getProperty(b));
    }
    
    private double diff(String va, String vb) {
        int l1 = (va == null ? 0 : va.length());
        int l2 = (vb == null ? 0 : vb.length());
        if (l1 == 0 || l2 == 0) return 0.0;
        Matcher m1 = iPattern.matcher(va);
        Matcher m2 = iPattern.matcher(vb);
        if (m1.matches() && m2.matches()) {
            String b1 = m1.group(1), b2 = m2.group(1);
            if (!b1.equals(b2)) return 1.0;
            int n1 = Integer.valueOf(m1.group(2)), n2 = Integer.valueOf(m2.group(2));
            if (n1 / 100 == n2 / 100)
                return Math.abs(n1 - n2) / 10000.0;
            return Math.abs(n1 - n2) / 500.0;
        }
        
        for (int i = 0; i < l1 && i < l2; i++) {
            if (va.charAt(i) != vb.charAt(i)) {
                return 1.0 - ((double)i) / Math.max(l1, l2);
            }
        }
        return 1.0 - ((double)Math.min(l1, l2)) / Math.max(l1, l2);
    }
    
    private boolean isSameFloor(Student a, Student b) {
        String va = getProperty(a), vb = getProperty(b);
        int l1 = (va == null ? 0 : va.length());
        int l2 = (vb == null ? 0 : vb.length());
        if (l1 == 0 || l2 == 0) return l1 == l2;
        Matcher m1 = iPattern.matcher(va);
        Matcher m2 = iPattern.matcher(vb);
        if (m1.matches() && m2.matches()) {
            String b1 = m1.group(1), b2 = m2.group(1);
            if (!b1.equals(b2)) return false;
            int n1 = Integer.valueOf(m1.group(2)), n2 = Integer.valueOf(m2.group(2));
            return (n1 / 100 == n2 / 100);
        }
        return false;
    }
    
    @Override
    public void getExtendedInfo(Assignment<Student, TeamAssignment> assignment, Map<String, String> info) {
        super.getExtendedInfo(assignment, info);
        info.put(getKey(), String.valueOf(getValue(assignment)));
        int pairs = 0, sameFloor = 0;
        for (Constraint<Student, TeamAssignment> c: getModel().constraints())
            if (c instanceof Team) {
                Team t = (Team)c;
                for (Student s1: t.getContext(assignment).getStudents())
                    for (Student s2: t.getContext(assignment).getStudents())
                        if (s1.compareTo(s2) < 0) {
                            pairs ++;
                            if (isSameFloor(s1, s2)) sameFloor++;
                        }
            }
        info.put(getKey() + " [Same Floor]", sDoubleFormat.format(100.0 * sameFloor / pairs) + "%");
    }
    
    public static void main(String[] args) throws Exception {
        String[] test = new String[] {
                "CARYE111b", "CARYE110b", "CARYE126a", "CARYE328a", "CARYNW206a", "CARYSE102a", ""
        };
        DecimalFormat df = new DecimalFormat("0.0000");
        RoomNearby f = new RoomNearby("X");
        for (String a: test)
            for (String b: test)
                if (a.compareTo(b) <= 0)
                    System.out.println(a + "," + b + "," + df.format(f.diff(a, b)));
    }
}