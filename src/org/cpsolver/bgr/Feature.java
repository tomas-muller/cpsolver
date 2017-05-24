package org.cpsolver.bgr;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.AbstractCriterion;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;

public class Feature extends AbstractCriterion<Student, TeamAssignment> {
    public String iKey = null;
    public String[] iFallbacks = null;
    
    Feature(String key, String... fallbacks) {
        iKey = key;
        iFallbacks = fallbacks;
    }
    
    public String getKey() { return iKey; }
    
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
        return ToolBox.equals(getProperty(a), getProperty(b)) ? 1.0 : 0.0;
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
    public void getInfo(Assignment<Student, TeamAssignment> assignment, Map<String, String> info) {
        super.getInfo(assignment, info);
        info.put(getKey(), String.valueOf(getValue(assignment)));
        Map<String, Integer> values = new HashMap<String, Integer>();
        for (Student student: getModel().variables()) {
            String value = getProperty(student);
            if (value != null) {
                Integer count = values.get(value);
                values.put(value, 1 + (count == null ? 0 : count.intValue()));
            }
        }
        TeamBuilder model = (TeamBuilder)getModel();
        Map<String, Map<Integer,Integer>> histo = new HashMap<String, Map<Integer,Integer>>();
        for (Team team: model.getTeams()) {
            Map<String, Integer> x = new HashMap<String, Integer>();
            for (Student student: team.getContext(assignment).getStudents()) {
                String value = getProperty(student);
                if (value != null) {
                    Integer count = x.get(value);
                    x.put(value, 1 + (count == null ? 0 : count.intValue()));
                }
            }
            for (Map.Entry<String, Integer> e: x.entrySet()) {
                Map<Integer,Integer> h = histo.get(e.getKey());
                if (h == null) {
                    h = new HashMap<Integer, Integer>();
                    histo.put(e.getKey(), h);
                }
                Integer count = h.get(e.getValue());
                h.put(e.getValue(), 1 + (count == null ? 0 : count.intValue()));
            }
        }
        int teams = model.getTeams().size();
        for (String value: values.keySet()) {
            int total = values.get(value);
            double avg = ((double)total) / teams;
            int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, penalty = 0;
            double rms = 0;
            for (Team t: model.getTeams()) {
                int cnt = 0;
                for (Student s: t.getContext(assignment).getStudents())
                    if (value.equals(getProperty(s))) cnt ++;
                if (cnt < min) min = cnt;
                if (cnt > max) max = cnt;
                rms = Math.pow(cnt - avg, 2.0);
                penalty += cnt * (cnt - 1) / 2;
            }
            if (max > 1)
                info.put(getKey() + " [" + value + "]", sDoubleFormat.format(Math.sqrt(rms / teams)) + " (" + total + ": " + min + ".." + max + ") p=" + penalty);
            if (histo.containsKey(value)) {
                int remain = teams;
                for (Integer i: histo.get(value).values()) remain -= i;
                if (remain != 0) histo.get(value).put(0, remain);
                info.put(getKey() + " H[" + value + "]", histo.get(value).toString());
            }
        }
    }

}
