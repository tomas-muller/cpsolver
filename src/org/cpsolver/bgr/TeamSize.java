package org.cpsolver.bgr;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.GlobalConstraint;

public class TeamSize extends GlobalConstraint<Student, TeamAssignment> {
    private int iSize = 0;
    
    public TeamSize(int size) { iSize = size; }
    
    public int getSize() { return iSize; }

    @Override
    public void computeConflicts(Assignment<Student, TeamAssignment> assignment, TeamAssignment ta, Set<TeamAssignment> conflicts) {
        Team team = ta.getTeam();
        Set<Student> students = team.getContext(assignment).getStudents();
        if (!students.contains(ta.variable()) && students.size() >= getSize()) {
            TeamAssignment adept = null; double best = 0.0;
            for (Student student: students) {
                TeamAssignment other = assignment.getValue(student); 
                double value = other.toDouble(assignment);
                if (adept == null || value > best) { adept = other; best = value; }
            }
            if (adept != null) conflicts.add(adept);
        }
    }
    
    @Override
    public boolean inConflict(Assignment<Student, TeamAssignment> assignment, TeamAssignment ta) {
        Set<Student> students = ta.getTeam().getContext(assignment).getStudents();
        return !students.contains(ta.variable()) && students.size() >= getSize();
    }
    
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
}