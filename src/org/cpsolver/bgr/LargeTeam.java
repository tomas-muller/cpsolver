package org.cpsolver.bgr;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.AbstractCriterion;
import org.cpsolver.ifs.util.DataProperties;

public class LargeTeam extends AbstractCriterion<Student, TeamAssignment> {
    private int iSize;
    private boolean iInternational;
    
    public LargeTeam(boolean international, int size) {
        iInternational = international;
        iSize = size;
    }
    
    @Override
    public String getAbbreviation() {
        return (iInternational ? "ILT" : "LT");
    }
    
    @Override
    public String getName() {
        return (iInternational ? "International " : "") + "Large Team";
    }
    
    @Override
    public String getWeightName() {
        return "Weight." + (iInternational ? "International" : "") + "LargeTeam";
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 1.0;
    }
    
    @Override
    public double getValue(Assignment<Student, TeamAssignment> assignment, TeamAssignment value, Set<TeamAssignment> conflicts) {
        if (iInternational && !value.variable().isInternational()) return 0.0;
        Set<Student> students = (iInternational ? value.getTeam().getContext(assignment).getInternationalStudents() : value.getTeam().getContext(assignment).getStudents());
        int size = students.size() + (students.contains(value.variable()) ? 0 : 1);
        return (size > iSize ? 1.0 : 0.0);
    }
    
    @Override
    public double getValue(Assignment<Student, TeamAssignment> assignment, Collection<Student> variables) {
        Set<Team> teams = new HashSet<Team>();
        double penalty = 0.0;
        for (Student student: variables) {
            if (iInternational && !student.isInternational()) continue;
            TeamAssignment ta = assignment.getValue(student);
            if (ta != null && teams.add(ta.getTeam())) {
                int size = (iInternational ? ta.getTeam().getContext(assignment).getInternationalStudents().size() : ta.getTeam().getContext(assignment).getStudents().size());
                if (size > iSize) penalty += (size - iSize);
            }
        }
        return penalty;
    }
    
    @Override
    public void getInfo(Assignment<Student, TeamAssignment> assignment, Map<String, String> info) {
        if (iInternational)
            info.put("International students in a team over " + iSize, getValue(assignment) + " (precise:" + getValue(assignment, getModel().variables()) + ")");
        else
            info.put("Students in a team over " + iSize, getValue(assignment) + " (precise:" + getValue(assignment, getModel().variables()) + ")");
    }

}
