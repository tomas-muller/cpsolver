package org.cpsolver.teams;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.GlobalConstraint;

public class SameFeature extends GlobalConstraint<Student, TeamAssignment> {
    public String iKey = null;
    public String[] iFallbacks = null;
    
    public SameFeature(String key, String[] fallbacks) {
        iKey = key;
        iFallbacks = fallbacks;
    }
    
    public SameFeature(String... keyWithFallbacks) {
        iKey = keyWithFallbacks[0];
        iFallbacks = new String[keyWithFallbacks.length - 1];
        for (int i = 0; i < keyWithFallbacks.length - 1; i++)
            iFallbacks[i] = keyWithFallbacks[1 + i];
    }
    
    public String getKey() { return iKey; }
    
    public String[] getFallbacks() { return iFallbacks; }
    
    public String getProperty(Student student) {
        String value = student.getProperty(iKey);
        if (value != null) return value;
        for (String key: iFallbacks) {
            value = student.getProperty(key);
            if (value != null) return value;
        }
        return value;
    }
    
    public boolean same(Student a, Student b) {
        String va = getProperty(a);
        String vb = getProperty(b);
        return (va == null || va.isEmpty() || vb == null || vb.isEmpty() || va.equals(vb));
    }
    

    @Override
    public void computeConflicts(Assignment<Student, TeamAssignment> assignment, TeamAssignment value, Set<TeamAssignment> conflicts) {
        Team.Context cx = value.getTeam().getContext(assignment);
        for (Student s: cx.getStudents()) {
            if (s.equals(value.variable())) continue;
            if (!same(value.variable(), s))
                conflicts.add(assignment.getValue(s));
        }
    }
    
    @Override
    public boolean inConflict(Assignment<Student, TeamAssignment> assignment, TeamAssignment value) {
        Team.Context cx = value.getTeam().getContext(assignment);
        for (Student s: cx.getStudents()) {
            if (s.equals(value.variable())) continue;
            if (!same(value.variable(), s)) return true;
        }
        return false;
    }

    @Override
    public boolean isConsistent(TeamAssignment first, TeamAssignment second) {
        return !first.getTeam().equals(second.getTeam()) || same(first.variable(), second.variable());
    }

}