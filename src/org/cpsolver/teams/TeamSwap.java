package org.cpsolver.teams;

import java.util.HashMap;
import java.util.Map;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;

public class TeamSwap implements NeighbourSelection<Student, TeamAssignment> {
    
    public TeamSwap(DataProperties config) {
    }

    @Override
    public void init(Solver<Student, TeamAssignment> solver) {
    }

    @Override
    public Neighbour<Student, TeamAssignment> selectNeighbour(Solution<Student, TeamAssignment> solution) {
        int r1 = ToolBox.random(solution.getModel().variables().size());
        for (int i = 0; i < solution.getModel().variables().size(); i++) {
            Student s1 = solution.getModel().variables().get((i + r1) % solution.getModel().variables().size());
            TeamAssignment a1 = solution.getAssignment().getValue(s1);
            if (a1 == null) continue;
            if (s1.values(solution.getAssignment()).size() <= 1) continue;
            if (s1.getSameTeam() != null) continue;
            boolean l1 = s1.isLeader();
            int r2 = ToolBox.random(solution.getModel().variables().size());
            for (int j = 0; j < solution.getModel().variables().size(); j++) {
                Student s2 = solution.getModel().variables().get((j + r2) % solution.getModel().variables().size());
                if (s1.equals(s2)) continue;
                if (s2.values(solution.getAssignment()).size() <= 1) continue;
                if (s2.getSameTeam() != null) continue;
                boolean l2 = s2.isLeader();
                if (l1 != l2) continue;
                TeamAssignment a2 = solution.getAssignment().getValue(s2);
                if (a2 == null || a1.getTeam().equals(a2.getTeam())) continue;
                if (!a1.getTeam().variables().contains(s2) || !a2.getTeam().variables().contains(s1)) continue;
                TeamAssignment n1 = new TeamAssignment(s1, a2.getTeam());
                TeamAssignment n2 = new TeamAssignment(s2, a1.getTeam());
                return new Swap(n1, n2, n1.toDouble(solution.getAssignment()) + n2.toDouble(solution.getAssignment()) - a1.toDouble(solution.getAssignment()) - a2.toDouble(solution.getAssignment()));
            }
        }
        return null;
    }
    
    public static class Swap implements Neighbour<Student, TeamAssignment> {
        private TeamAssignment iA1, iA2;
        private double iValue;
        
        public Swap(TeamAssignment a1, TeamAssignment a2, double value) {
            iA1 = a1; iA2 = a2; iValue = value;
        }

        @Override
        public void assign(Assignment<Student, TeamAssignment> assignment, long iteration) {
            assignment.unassign(iteration, iA1.variable());
            assignment.unassign(iteration, iA2.variable());
            assignment.assign(iteration, iA1);
            assignment.assign(iteration, iA2);
        }

        @Override
        public double value(Assignment<Student, TeamAssignment> assignment) {
            return iValue;
        }

        @Override
        public Map<Student, TeamAssignment> assignments() {
            Map<Student, TeamAssignment> ret = new HashMap<Student, TeamAssignment>();
            ret.put(iA1.variable(), iA1);
            ret.put(iA2.variable(), iA2);
            return ret;
        }
        
    }

}
