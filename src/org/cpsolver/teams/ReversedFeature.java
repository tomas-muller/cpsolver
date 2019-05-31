package org.cpsolver.teams;

import java.util.HashMap;
import java.util.Map;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;

public class ReversedFeature extends Feature {
    public int iMaximum = 0;
    
    public ReversedFeature(String key, String[] fallbacks) {
        super(key, fallbacks);
    }
    
    public ReversedFeature(String... keyWithFallbacks) {
        super(keyWithFallbacks);
    }
    
    @Override
    public double similar(Student a, Student b) {
        String va = getProperty(a);
        String vb = getProperty(b);
        if (va == null || vb == null) return 0.0;
        return va.equals(vb) ? 0.0 : 1.0;
    }
    
    @Override
    public void getExtendedInfo(Assignment<Student, TeamAssignment> assignment, Map<String, String> info) {
        super.getExtendedInfo(assignment, info);
        info.put(getKey(), String.valueOf(getValue(assignment)));
        Map<String, Integer> values = new HashMap<String, Integer>();
        for (Student student: getModel().variables()) {
            String value = getProperty(student);
            if (value != null) {
                Integer count = values.get(value);
                values.put(value, 1 + (count == null ? 0 : count.intValue()));
            }
        }
        for (String value: values.keySet()) {
            int total = values.get(value);
            int teams = 0;
            for (Constraint<Student, TeamAssignment> c: getModel().constraints())
                if (c instanceof Team) {
                    Team t = (Team)c;
                    int cnt = 0;
                    for (Student s: t.getContext(assignment).getStudents())
                        if (value.equals(getProperty(s))) cnt ++;
                    if (cnt > 0)
                        teams ++;
                }
            double avg = ((double)total) / teams;
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, penalty = 0;
            double rms = 0;
            for (Constraint<Student, TeamAssignment> c: getModel().constraints()) {
                if (c instanceof Team) {
                    Team t = (Team)c;
                    int cnt = 0;
                    for (Student s: t.getContext(assignment).getStudents())
                        if (value.equals(getProperty(s))) cnt ++;
                    if (cnt == 0) continue;
                    if (cnt < min) min = cnt;
                    if (cnt > max) max = cnt;
                    rms += Math.pow(cnt - avg, 2.0);
                    penalty += cnt * (cnt - 1) / 2;
                }
            }
            if (teams > 1)
                info.put(getKey() + " [" + value + "]", sDoubleFormat.format(((double)total)/teams) + " +/- " + sDoubleFormat.format(Math.sqrt(rms / teams)) + " (" + total + "/" + teams + ": " + min + ".." + max + ") p=" + penalty);
        }
    }
}