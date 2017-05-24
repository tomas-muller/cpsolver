package org.cpsolver.bgr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Variable;

public class Student extends Variable<Student, TeamAssignment> {
    private Map<String,String> iProperties = new HashMap<String,String>();
    
    public void setProperty(String key, String value) {
        if (value == null) iProperties.remove(key);
        else iProperties.put(key, value);
    }
    
    public String getProperty(String key) {
        return iProperties.get(key);
    }
    
    @Override
    public List<TeamAssignment> values(Assignment<Student, TeamAssignment> assignment) {
        List<TeamAssignment> values = new ArrayList<TeamAssignment>();
        // boolean international = InternationalTeamLead.isInternational(this);
        for (Team team: ((TeamBuilder)getModel()).getTeams()) {
            // if (international && !InternationalTeamLead.isInternational(((Team)c).getTeamLead())) continue;
            values.add(new TeamAssignment(this, team));
        }
        return values;
    }
    
    @Override
    public void variableAssigned(Assignment<Student, TeamAssignment> assignment, long iteration, TeamAssignment ta) {
        super.variableAssigned(assignment, iteration, ta);
        ta.getTeam().getContext(assignment).assigned(assignment, ta);
    }

    @Override
    public void variableUnassigned(Assignment<Student, TeamAssignment> assignment, long iteration, TeamAssignment ta) {
        super.variableUnassigned(assignment, iteration, ta);
        ta.getTeam().getContext(assignment).unassigned(assignment, ta);
    }
    
    public static boolean isInternational(Student student) {
        return "Yes".equalsIgnoreCase(student.getProperty("BGRi")) || "True".equalsIgnoreCase(student.getProperty("TLi")) || "TRUE".equalsIgnoreCase(student.getProperty("TSi"));
    }

    Boolean iInternational = null;
    public boolean isInternational() {
        if (iInternational == null)
            iInternational = isInternational(this);
        return iInternational;
    }
    
    @Override
    public String toString() {
        return getProperty("PUID");
    }
}
