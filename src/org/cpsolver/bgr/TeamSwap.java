package org.cpsolver.bgr;

import java.util.HashMap;
import java.util.Map;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.ifs.model.LazySwap;
import org.cpsolver.ifs.model.Model;
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
        Model<Student, TeamAssignment> model = solution.getModel();
        Assignment<Student, TeamAssignment> assignment = solution.getAssignment();
        int r1 = ToolBox.random(model.variables().size());
        for (int i = 0; i < model.variables().size(); i++) {
            Student s1 = model.variables().get((i + r1) % model.variables().size());
            TeamAssignment a1 = assignment.getValue(s1);
            if (a1 == null) continue;
            int r2 = ToolBox.random(model.variables().size());
            j: for (int j = 0; j < model.variables().size(); j++) {
                Student s2 = model.variables().get((j + r2) % model.variables().size());
                if (s1.equals(s2)) continue;
                TeamAssignment a2 = assignment.getValue(s2);
                if (a2 == null || a1.getTeam().equals(a2.getTeam())) continue;
                TeamAssignment n1 = new TeamAssignment(s1, a2.getTeam());
                TeamAssignment n2 = new TeamAssignment(s2, a1.getTeam());
                for (GlobalConstraint<Student, TeamAssignment> g: model.globalConstraints()) {
                    if (g instanceof TeamSize) continue;
                    if (g.inConflict(assignment, n1) || g.inConflict(assignment, n2)) continue j;
                }
                return new LazySwap<Student, TeamAssignment>(n1, n2);
                //return new Swap(n1, n2, n1.toDouble(assignment) + n2.toDouble(assignment) - a1.toDouble(assignment) - a2.toDouble(assignment));
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
