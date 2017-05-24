package org.cpsolver.teams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;
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
        for (Constraint<Student, TeamAssignment> c: getModel().constraints()) {
            if (c instanceof Team)
                values.add(new TeamAssignment(this, (Team)c));
        }
        return values;
    }
}
