package org.cpsolver.teams;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;

public class ProportionalFeature extends Feature {

    public ProportionalFeature(String key, String[] fallbacks) {
        super(key, fallbacks);
    }
    
    public ProportionalFeature(String... keyWithFallbacks) {
        super(keyWithFallbacks);
    }
    
    /*
    @Override
    public double similar(Student a, Student b) {
        return ToolBox.equals(getProperty(a), getProperty(b)) ? 1.0 : 0.0;
    }
    */
    
    @Override
    public double getValue(Assignment<Student, TeamAssignment> assignment, TeamAssignment value, Set<TeamAssignment> conflicts) {
        double similar = 0;
        for (Student student: value.getTeam().getContext(assignment).getStudents()) {
            if (student.equals(value.variable())) continue;
            similar += similar(student, value.variable());
        }
        return similar / value.getTeam().getSize();
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
        int teams = 0;
        for (Constraint<Student, TeamAssignment> c: getModel().constraints()) if (c instanceof Team) teams ++;
        for (String value: values.keySet()) {
            int total = values.get(value);
            double target = ((double) total) / getModel().countVariables();
            double min = 1.0, max = 0.0, penalty = 0;
            double rms = 0;
            for (Constraint<Student, TeamAssignment> c: getModel().constraints()) {
                if (c instanceof Team) {
                    Team t = (Team)c;
                    double cnt = 0;
                    for (Student s: t.getContext(assignment).getStudents())
                        if (value.equals(getProperty(s))) cnt ++;
                    double ratio = (cnt / t.getContext(assignment).getStudents().size());
                    if (ratio < min) min = ratio;
                    if (ratio > max) max = ratio;
                    rms += Math.pow(ratio - target, 2.0);
                    penalty += cnt * (cnt - 1) / t.getSize() / 2;
                }
            }
            info.put(getKey() + " [" + value + "]", sDoubleFormat.format(100.0 * Math.sqrt(rms / teams)) + "% (" + sDoubleFormat.format(100.0 * target) + "%: " + sDoubleFormat.format(100.0 * min) + "% .. " + sDoubleFormat.format(100.0 * max) + "%) p=" + sDoubleFormat.format(penalty));
        }
    }

}
