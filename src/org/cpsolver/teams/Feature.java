package org.cpsolver.teams;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.AbstractCriterion;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.util.DataProperties;

public class Feature extends AbstractCriterion<Student, TeamAssignment> {
    public String iKey = null;
    public String[] iFallbacks = null;
    
    public Feature(String key, String[] fallbacks) {
        iKey = key;
        iFallbacks = fallbacks;
    }
    
    public Feature(String... keyWithFallbacks) {
        iKey = keyWithFallbacks[0];
        iFallbacks = new String[keyWithFallbacks.length - 1];
        for (int i = 0; i < keyWithFallbacks.length - 1; i++)
            iFallbacks[i] = keyWithFallbacks[1 + i];
    }
    
    public String getKey() { return iKey; }
    
    public String[] getFallbacks() { return iFallbacks; }
    
    @Override
    public String getName() {
        return getKey();
    }
    
    @Override
    public String getWeightName() {
        return "Weight." + getKey();
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 1.0;
    }
    
    public String getProperty(Student student) {
        String value = student.getProperty(iKey);
        if (value != null) return value;
        for (String key: iFallbacks) {
            value = student.getProperty(key);
            if (value != null) return value;
        }
        return value;
    }
    
    public double similar(Student a, Student b) {
        String va = getProperty(a);
        String vb = getProperty(b);
        if (va == null || vb == null) return 0.0;
        return va.equals(vb) ? 1.0 : 0.0;
    }

    @Override
    public double getValue(Assignment<Student, TeamAssignment> assignment, TeamAssignment value, Set<TeamAssignment> conflicts) {
        double similar = 0;
        for (Student student: value.getTeam().getContext(assignment).getStudents()) {
            if (student.equals(value.variable())) continue;
            similar += similar(student, value.variable());
        }
        return similar;
    }
    
    @Override
    public double getValue(Assignment<Student, TeamAssignment> assignment, Collection<Student> variables) {
        return super.getValue(assignment, variables) / 2.0;
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
        int teams = 0;
        for (Constraint<Student, TeamAssignment> c: getModel().constraints()) if (c instanceof Team) teams ++;
        for (String value: values.keySet()) {
            int total = values.get(value);
            double avg = ((double)total) / teams;
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, penalty = 0;
            double rms = 0;
            for (Constraint<Student, TeamAssignment> c: getModel().constraints()) {
                if (c instanceof Team) {
                    Team t = (Team)c;
                    int cnt = 0;
                    for (Student s: t.getContext(assignment).getStudents())
                        if (value.equals(getProperty(s))) cnt ++;
                    if (cnt < min) min = cnt;
                    if (cnt > max) max = cnt;
                    rms += Math.pow(cnt - avg, 2.0);
                    penalty += cnt * (cnt - 1) / 2;
                }
            }
            if (max > 1)
                info.put(getKey() + " [" + value + "]", sDoubleFormat.format(Math.sqrt(rms / teams)) + " (" + total + ": " + min + ".." + max + ") p=" + penalty);
        }
    }

}
