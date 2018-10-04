package org.cpsolver.studentsct.heuristics.selection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Lock;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.BacktrackNeighbourSelection;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.studentsct.StudentSectioningModel;
import org.cpsolver.studentsct.model.CourseRequest;
import org.cpsolver.studentsct.model.Enrollment;
import org.cpsolver.studentsct.model.Request;

/**
 * Swap enrollments of different students of a course. This is to improve
 * the enrollment alternativity {@link Enrollment#getPriority()} as well as
 * selection preferences {@link Enrollment#percentSelected()}. 
 * 
 * <br>
 * <br>
 * 
 * @version StudentSct 1.3 (Student Sectioning)<br>
 *          Copyright (C) 2007 - 2018 Tomas Muller<br>
 *          <a href="mailto:muller@unitime.org">muller@unitime.org</a><br>
 *          <a href="http://muller.unitime.org">http://muller.unitime.org</a><br>
 * <br>
 *          This library is free software; you can redistribute it and/or modify
 *          it under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation; either version 3 of the
 *          License, or (at your option) any later version. <br>
 * <br>
 *          This library is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details. <br>
 * <br>
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with this library; if not see
 *          <a href='http://www.gnu.org/licenses/'>http://www.gnu.org/licenses/</a>.
 */

public class StudentEnrollmentSwapSelection implements NeighbourSelection<Request, Enrollment> {
    private Selection iSelection = null;
    protected Queue<Request> iRequests = null;

    public StudentEnrollmentSwapSelection(DataProperties properties) {
    }

    public void init(Solver<Request, Enrollment> solver, String name) {
        List<Request> variables = new ArrayList<Request>(solver.currentSolution().getModel().assignedVariables(solver.currentSolution().getAssignment()));
        Collections.shuffle(variables);
        iRequests = new LinkedList<Request>(variables);
        if (iSelection == null) {
            try {
                iSelection = new Selection(solver.getProperties());
                iSelection.init(solver);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        Progress.getInstance(solver.currentSolution().getModel()).setPhase(name, variables.size());
    }

    @Override
    public void init(Solver<Request, Enrollment> solver) {
        init(solver, "Enrollment swaps...");
    }
    
    protected synchronized Request nextRequest() {
        return iRequests.poll();
    }
    
    public synchronized void addRequest(Request request) {
        if (iRequests != null) iRequests.add(request);
    }

    @Override
    public Neighbour<Request, Enrollment> selectNeighbour(Solution<Request, Enrollment> solution) {
        Request request = null;
        while ((request = nextRequest()) != null) {
            Progress.getInstance(solution.getModel()).incProgress();
            if (request instanceof CourseRequest) {
                try {
                    Enrollment e = request.getAssignment(solution.getAssignment());
                    if (e.getPriority() > 0 || !((CourseRequest)request).getSelectedChoices().isEmpty()) {
                        Neighbour<Request, Enrollment> n = iSelection.selectNeighbour(solution, request);
                        if (n != null && n.value(solution.getAssignment()) <= 0.0)
                            return n;
                    }
                } catch (ConcurrentModificationException e) {
                    addRequest(request);
                }
            }
        }
        return null;
    }
    
    class Selection extends BacktrackNeighbourSelection<Request, Enrollment> {
        private int iMaxValues = 1000;
        
        Selection(DataProperties properties) throws Exception {
            super(properties);
            setTimeout(properties.getPropertyInt("Neighbour.EnrollmentSwapTimeout", 500));
            iMaxValues = properties.getPropertyInt("Neighbour.EnrollmentSwapMaxValues", iMaxValues);
        }
        
        @Override
        protected Iterator<Enrollment> values(BacktrackNeighbourSelection<Request, Enrollment>.BacktrackNeighbourSelectionContext context, Request variable) {
            if (variable instanceof CourseRequest) {
                final CourseRequest request = (CourseRequest)variable;
                final StudentSectioningModel model = (StudentSectioningModel)context.getModel();
                final Assignment<Request, Enrollment> assignment = context.getAssignment();
                List<Enrollment> values = null;
                Enrollment current = request.getAssignment(context.getAssignment());
                if (!request.getSelectedChoices().isEmpty() && current != null && current.getPriority() == 0) {
                    values = request.getSelectedEnrollments(assignment, false);
                } else {
                    values = (iMaxValues > 0 ? request.computeRandomEnrollments(assignment, iMaxValues) : request.computeEnrollments(assignment));
                }
                Collections.sort(values, new Comparator<Enrollment>() {
                    private HashMap<Enrollment, Double> iValues = new HashMap<Enrollment, Double>();
                    private Double value(Enrollment e) {
                        Double value = iValues.get(e);
                        if (value == null) {
                            value = model.getStudentWeights().getWeight(assignment, e,
                                    (model.getDistanceConflict() == null ? null : model.getDistanceConflict().conflicts(e)),
                                    (model.getTimeOverlaps() == null ? null : model.getTimeOverlaps().conflicts(e)));
                            iValues.put(e, value);
                        }
                        return value;
                    }
                    @Override
                    public int compare(Enrollment e1, Enrollment e2) {
                        if (e1.equals(assignment.getValue(request))) return -1;
                        if (e2.equals(assignment.getValue(request))) return 1;
                        Double v1 = value(e1), v2 = value(e2);
                        return v1.equals(v2) ? e1.compareTo(assignment, e2) : v2.compareTo(v1);
                    }
                });
                return values.iterator();
            } else {
                return variable.computeEnrollments(context.getAssignment()).iterator();
            }
        }
        
        @Override
        protected void selectNeighbour(Solution<Request, Enrollment> solution, Request variable, BacktrackNeighbourSelectionContext context) {
            Lock lock = solution.getLock().writeLock();
            lock.lock();
            try {
                exploreEnrollmentSwaps(context, variable);
            } finally {
                lock.unlock();
            }
        }
        
        protected void exploreEnrollmentSwaps(BacktrackNeighbourSelectionContext context, Request variable) {
            Enrollment current = context.getAssignment().getValue(variable);
            double currentValue = (current == null ? 0.0 : current.toDouble(context.getAssignment()));
            for (Iterator<Enrollment> e = values(context, variable); canContinueEvaluation(context) && e.hasNext();) {
                Enrollment value = e.next();
                if (value.equals(current)) continue;
                if (current != null && currentValue <= value.toDouble(context.getAssignment())) continue;
                if (context.isTimeoutReached() || context.isMaxItersReached()) break;
                context.incIteration();
                if (context.getModel().inConflict(context.getAssignment(), value)) {
                    for (Enrollment other: new ArrayList<Enrollment>(value.getCourse().getContext(context.getAssignment()).getEnrollments())) {
                        if (other.getStudent().equals(value.getStudent()) || !other.getSections().equals(value.getSections())) continue;
                        context.getAssignment().unassign(0, other.variable());
                        if (!context.getModel().inConflict(context.getAssignment(), value)) {
                            if (current != null)
                                context.getAssignment().unassign(0, current.variable());
                            context.getAssignment().assign(0, value);
                            for (Iterator<Enrollment> f = values(context, other.variable()); canContinueEvaluation(context) && f.hasNext();) {
                                Enrollment fix = f.next();
                                if (!context.getModel().inConflict(context.getAssignment(), fix)) {
                                    context.getAssignment().assign(0, fix);
                                    context.saveBest(variable, other.variable());
                                    context.getAssignment().unassign(0, fix.variable());
                                }
                            }
                            if (current == null)
                                context.getAssignment().unassign(0, variable);
                            else
                                context.getAssignment().assign(0, current);
                        }
                        context.getAssignment().assign(0, other);
                    }
                } else {
                    if (current != null)
                        context.getAssignment().unassign(0, current.variable());
                    context.getAssignment().assign(0, value);
                    context.saveBest(variable);
                    if (current == null)
                        context.getAssignment().unassign(0, variable);
                    else
                        context.getAssignment().assign(0, current);
                }
            }
        }        
    }

}