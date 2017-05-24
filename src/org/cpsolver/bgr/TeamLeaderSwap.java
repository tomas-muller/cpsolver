package org.cpsolver.bgr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.LazyNeighbour;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;

public class TeamLeaderSwap implements NeighbourSelection<Student, TeamAssignment> {
    
    public TeamLeaderSwap(DataProperties config) {
    }

    @Override
    public void init(Solver<Student, TeamAssignment> solver) {
    }

    @Override
    public Neighbour<Student, TeamAssignment> selectNeighbour(Solution<Student, TeamAssignment> solution) {
        TeamBuilder model = (TeamBuilder)solution.getModel();
        Assignment<Student, TeamAssignment> assignment = solution.getAssignment();
        int r1 = ToolBox.random(model.getTeams().size());
        for (int i = 0; i < model.getTeams().size(); i++) {
            Team t1 = model.getTeams().get((i + r1) % model.getTeams().size());
            int r2 = ToolBox.random(model.getTeams().size());
            for (int j = 0; j < model.getTeams().size(); j++) {
                Team t2 = model.getTeams().get((j + r2) % model.getTeams().size());
                if (t1.equals(t2) || t1.getTeamLead().isInternational() != t2.getTeamLead().isInternational()) continue;
                return new Swap(assignment, t1, t2);
            }
        }
        return null;
    }
    
    public static class Swap extends LazyNeighbour<Student, TeamAssignment> {
        private Team iT1, iT2;
        private List<Student> iS1, iS2;
        
        public Swap(Assignment<Student, TeamAssignment> assignment, Team t1, Team t2) {
            iT1 = t1; iT2 = t2;
            iS1 = new ArrayList<Student>(t1.getContext(assignment).getStudents());
            iS2 = new ArrayList<Student>(t2.getContext(assignment).getStudents());
        }

        @Override
        protected void doAssign(Assignment<Student, TeamAssignment> assignment, long iteration) {
            for (Student s: iS1)
                assignment.unassign(iteration, s);
            for (Student s: iS2)
                assignment.unassign(iteration, s);
            for (Student s: iS1)
                assignment.assign(iteration, new TeamAssignment(s, iT2));
            for (Student s: iS2)
                assignment.assign(iteration, new TeamAssignment(s, iT1));
        }

        @Override
        protected void undoAssign(Assignment<Student, TeamAssignment> assignment, long iteration) {
            for (Student s: iS1)
                assignment.unassign(iteration, s);
            for (Student s: iS2)
                assignment.unassign(iteration, s);
            for (Student s: iS1)
                assignment.assign(iteration, new TeamAssignment(s, iT1));
            for (Student s: iS2)
                assignment.assign(iteration, new TeamAssignment(s, iT2));
        }

        @Override
        public Model<Student, TeamAssignment> getModel() {
            return iT1.getModel();
        }

        @Override
        public Map<Student, TeamAssignment> assignments() {
            Map<Student, TeamAssignment> map = new HashMap<Student, TeamAssignment>();
            for (Student s: iS1)
                map.put(s, new TeamAssignment(s, iT2));
            for (Student s: iS2)
                map.put(s, new TeamAssignment(s, iT1));
            return map;
        }
    }
}
