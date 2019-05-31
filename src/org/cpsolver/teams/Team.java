package org.cpsolver.teams;

import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;
import org.cpsolver.ifs.util.ToolBox;

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
    public String toString() {
        return iName;
    }

    @Override
    public void computeConflicts(Assignment<Student, TeamAssignment> assignment, TeamAssignment ta, Set<TeamAssignment> conflicts) {
        if (!ta.getTeam().equals(this)) return;
        Context cx = getContext(assignment);
        int count = cx.getStudentCount(ta.variable());
        while (count > getSize()) {
            Student student = ToolBox.random(cx.getStudents());
            conflicts.add(assignment.getValue(student));
            count -= student.getWeight();
        }
    }
    
    public class Context implements AssignmentConstraintContext<Student, TeamAssignment>{
        private Set<Student> iStudents = new HashSet<Student>();
        private int iCount = 0;
        
        public Context(Assignment<Student, TeamAssignment> assignment) {
            for (Student student: getModel().variables()) {
                TeamAssignment ta = assignment.getValue(student);
                if (ta != null && ta.getTeam().equals(Team.this)) {
                    iStudents.add(student);
                    iCount += student.getWeight();
                }
            }
        }

        @Override
        public void assigned(Assignment<Student, TeamAssignment> assignment, TeamAssignment ta) {
            if (!ta.getTeam().equals(Team.this)) return;
            if (iStudents.add(ta.variable()))
                iCount += ta.variable().getWeight();
            
        }

        @Override
        public void unassigned(Assignment<Student, TeamAssignment> assignment, TeamAssignment ta) {
            if (!ta.getTeam().equals(Team.this)) return;
            if (iStudents.remove(ta.variable()))
                iCount -= ta.variable().getWeight();
        }
        
        public Set<Student> getStudents() { return iStudents; }
        
        public int getStudentCount() { return iCount; }
        
        public int getStudentCount(Student student) { 
            if (student == null || iStudents.contains(student))
                return iCount;
            else
                return iCount + student.getWeight();
        }
    }

    @Override
    public Context createAssignmentContext(Assignment<Student, TeamAssignment> assignment) {
        return new Context(assignment);
    }
}
