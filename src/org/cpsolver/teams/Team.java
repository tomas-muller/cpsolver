package org.cpsolver.teams;

import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;

public class Team extends ConstraintWithContext<Student, TeamAssignment, Team.Context>{
    private String iName;
    private int iSize;
    
    public Team(Long id, String name, int size) {
        iId = id;
        iName = name;
        iSize = size;
    }
    
    @Override
    public String getName() {
        return iName;
    }
    
    public int getSize() {
        return iSize;
    }

    @Override
    public void computeConflicts(Assignment<Student, TeamAssignment> assignment, TeamAssignment ta, Set<TeamAssignment> conflicts) {
        if (!ta.getTeam().equals(this)) return;
        Set<Student> students = getContext(assignment).getStudents();
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
    
    public class Context implements AssignmentConstraintContext<Student, TeamAssignment>{
        private Set<Student> iStudents = new HashSet<Student>();
        
        public Context(Assignment<Student, TeamAssignment> assignment) {
            for (Student student: getModel().variables()) {
                TeamAssignment ta = assignment.getValue(student);
                if (ta != null && ta.getTeam().equals(Team.this))
                    iStudents.add(student);
            }
        }

        @Override
        public void assigned(Assignment<Student, TeamAssignment> assignment, TeamAssignment ta) {
            if (!ta.getTeam().equals(Team.this)) return;
            iStudents.add(ta.variable());
        }

        @Override
        public void unassigned(Assignment<Student, TeamAssignment> assignment, TeamAssignment ta) {
            if (!ta.getTeam().equals(Team.this)) return;
            iStudents.remove(ta.variable());
        }
        
        public Set<Student> getStudents() { return iStudents; }
    }

    @Override
    public Context createAssignmentContext(Assignment<Student, TeamAssignment> assignment) {
        return new Context(assignment);
    }
}
