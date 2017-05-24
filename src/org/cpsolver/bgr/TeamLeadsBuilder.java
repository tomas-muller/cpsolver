package org.cpsolver.bgr;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.DefaultParallelAssignment;
import org.cpsolver.ifs.assignment.DefaultSingleAssignment;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solution.SolutionListener;
import org.cpsolver.ifs.solver.ParallelSolver;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.CSVFile;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;

public class TeamLeadsBuilder extends TeamBuilder {
    private static Logger sLog = Logger.getLogger(TeamLeadsBuilder.class);
    
    public static void main(String[] args) throws IOException {
        DataProperties config = new DataProperties();
        config.load(TeamBuilder.class.getClass().getResourceAsStream("/org/cpsolver/bgr/leads.properties"));
        config.putAll(System.getProperties());
        ToolBox.configureLogging();
        
        TeamBuilder model = new TeamBuilder();
        
        model.loadData(config);
        
        double avgTeamSize = ((double)model.variables().size()) / model.getTeams().size();
        int teamSize = 1 + (int)Math.ceil(((double)model.variables().size()) / model.getTeams().size());
        int internationalTeamSize = (int)Math.ceil(((double)model.countInternationalStudents()) / model.countInternationalTeams());
        
        PrintWriter out = new PrintWriter(config.getProperty("Output.Text"));
        sLog.info("Team Size: " + teamSize + " (" + model.variables().size() + " students, " + model.getTeams().size() + " teams)");
        out.println("Team Size: " + teamSize + " (" + model.variables().size() + " students, " + model.getTeams().size() + " teams)");
        Map<String, List<Student>> rhg2students = model.getHallGroup2Students(false);
        for (Map.Entry<String, List<Team>> e: model.getHallGroup2Teams(false).entrySet()) {
            String rhg = e.getKey();
            List<Team> teams = e.getValue();
            List<Student> students = rhg2students.get(rhg);
            if (teams.isEmpty()) continue;
            if (students != null && students.size() > teamSize * teams.size())
                System.err.println("~ " + Math.round(avgTeamSize * teams.size()) + " team leaders in " + rhg + " (" + teams.size() + " supervisors, " + students.size() + " team leads already assigned)");
            else
                System.out.println("~ " + Math.round(avgTeamSize * teams.size()) + " team leaders in " + rhg + " (" + teams.size() + " supervisors" + (students == null ? "" : ", " + students.size() + " team leads already assigned") + ")");
            out.println("~ " + Math.round(avgTeamSize * teams.size()) + " team leaders in " + rhg + " (" + teams.size() + " supervisors" + (students == null ? "" : ", " + students.size() + " team leads already assigned") + ")");
        }
        out.println();
        
        sLog.info("International Team Size: " + internationalTeamSize + " (" + model.countInternationalStudents() + " students, " + model.countInternationalTeams() + " teams)");
        out.println("International Team Size: " + internationalTeamSize + " (" + model.countInternationalStudents() + " students, " + model.countInternationalTeams() + " teams)");
        Map<String, List<Student>> rhg2interns = model.getHallGroup2Students(true);
        for (Map.Entry<String, List<Team>> e: model.getHallGroup2Teams(true).entrySet()) {
            String rhg = e.getKey();
            List<Team> teams = e.getValue();
            List<Student> students = rhg2interns.get(rhg);
            if (teams.isEmpty()) continue;
            if (students != null && students.size() > internationalTeamSize * teams.size())
                System.err.println("~ " + Math.round(avgTeamSize * teams.size()) + " international team leaders in " + rhg + " (" + teams.size() + " international supervisors, " + students.size() + " team leads already assigned)");
            else
                System.out.println("~ " + Math.round(avgTeamSize * teams.size()) + " international team leaders in " + rhg + " (" + teams.size() + " international supervisors" + (students == null ? "" : ", " + students.size() + " international team leads already assigned") + ")");
            out.println("~ " + Math.round(avgTeamSize * teams.size()) + " international team leaders in " + rhg + " (" + teams.size() + " international supervisors" + (students == null ? "" : ", " + students.size() + " international team leads already assigned") + ")");
        }
        out.println();out.flush();
        
        model.addGlobalConstraint(new InternationalTeamLead());
        model.addGlobalConstraint(new RequiredFeature(RequiredFeature.Mode.SOFT_SUPERVISORS, "BGRHallGroup"));
        model.addGlobalConstraint(new RequiredFeature("RetreatDate"));
        model.addGlobalConstraint(new TeamSize(teamSize));
        if (internationalTeamSize < teamSize)
            model.addGlobalConstraint(new InternationalTeamSize(internationalTeamSize));
        
        model.addCriterion(new Feature("Gender"){});
        model.addCriterion(new Feature("Ethnicity"){});
        model.addCriterion(new Feature("International","TLi","BGRi"){});
        model.addCriterion(new LargeTeam(false, (int)Math.ceil(((double)model.variables().size()) / model.getTeams().size())));
        model.addCriterion(new SmallTeam(false, (int)Math.floor(((double)model.variables().size()) / model.getTeams().size())));
        model.addCriterion(new LargeTeam(true, (int)Math.ceil(((double)model.countInternationalStudents()) / model.countInternationalTeams())) {});
        model.addCriterion(new SmallTeam(true, (int)Math.floor(((double)model.countInternationalStudents()) / model.countInternationalTeams())) {});
        
        int nrSolvers = config.getPropertyInt("Parallel.NrSolvers", 1);
        Solver<Student, TeamAssignment> solver = (nrSolvers == 1 ? new Solver<Student, TeamAssignment>(config) : new ParallelSolver<Student, TeamAssignment>(config));
        
        Assignment<Student, TeamAssignment> assignment = (nrSolvers <= 1 ? new DefaultSingleAssignment<Student, TeamAssignment>() : new DefaultParallelAssignment<Student, TeamAssignment>());
        
        solver.setInitalSolution(new Solution<Student, TeamAssignment>(model, assignment));
        
        solver.currentSolution().addSolutionListener(new SolutionListener<Student, TeamAssignment>() {
            private boolean iFirst = false;
            @Override
            public void solutionUpdated(Solution<Student, TeamAssignment> solution) {
            }

            @Override
            public void getInfo(Solution<Student, TeamAssignment> solution, Map<String, String> info) {
            }

            @Override
            public void getInfo(Solution<Student, TeamAssignment> solution, Map<String, String> info, Collection<Student> variables) {
            }

            @Override
            public void bestCleared(Solution<Student, TeamAssignment> solution) {
            }

            @Override
            public void bestSaved(Solution<Student, TeamAssignment> solution) {
                Model<Student, TeamAssignment> m = solution.getModel();
                Assignment<Student, TeamAssignment> a = solution.getAssignment();
                System.out.println("**BEST[" + solution.getIteration() + "]** " + m.toString(a));
                if (a.unassignedVariables(m).isEmpty() && !iFirst) {
                    sLog.info("Info: " + ToolBox.dict2string(solution.getExtendedInfo(), 2));
                    iFirst = true;
                }
            }

            @Override
            public void bestRestored(Solution<Student, TeamAssignment> solution) {
            }
        });
        
        solver.start();
        try {
            solver.getSolverThread().join();
        } catch (InterruptedException e) {
        }
        
        Solution<Student, TeamAssignment> solution = solver.lastSolution();
        solution.restoreBest();

        sLog.info("Best solution found after " + solution.getBestTime() + " seconds (" + solution.getBestIteration() + " iterations).");
        sLog.info("Number of assigned variables is " + solution.getModel().assignedVariables(solution.getAssignment()).size());
        sLog.info("Total value of the solution is " + solution.getModel().getTotalValue(solution.getAssignment()));

        out.println("Info: " + ToolBox.dict2string(solution.getExtendedInfo(), 2));
        
        String header = "PUID,International,BGRHallGroup,RetreatDate";
        for (Criterion<Student, TeamAssignment> criterion: model.getCriteria())
            if (criterion instanceof Feature)
                header += "," + ((Feature)criterion).getKey();
        out.println(header);
        for (Team t: model.getTeams()) {
            Team.Context cx = t.getContext(solution.getAssignment());
            String hline = t.getTeamLead().getProperty("PUID") + "," + t.getTeamLead().isInternational() + "," + t.getTeamLead().getProperty("BGRHallGroup")
                    + "," + t.getTeamLead().getProperty("RetreatDate");
            for (Criterion<Student, TeamAssignment> criterion: model.getCriteria())
                if (criterion instanceof Feature) {
                    String value = ((Feature)criterion).getProperty(t.getTeamLead());
                    hline += ",\"" + (value == null ? "" : value) + "\"";
                }
            out.println(hline);
            int idx = 1;
            for (Student s: cx.getStudents()) {
                String line = "  [" + sCounterFormat.format(idx++) + "]  " + s.getProperty("PUID") + "," + s.isInternational() + "," + s.getProperty("BGRHallGroup") +
                        "," + s.getProperty("RetreatDate");
                for (Criterion<Student, TeamAssignment> criterion: model.getCriteria())
                    if (criterion instanceof Feature) {
                        String value = ((Feature)criterion).getProperty(s);
                        line += ",\"" + (value == null ? "" : value) + "\"";
                    }
                out.println(line);
            }
        }
        out.flush();
        out.close();
        
        CSVFile output = new CSVFile();
        List<CSVFile.CSVField> outputHeader = new ArrayList<CSVFile.CSVField>();
        for (CSVFile.CSVField head: model.getTeamFields())
            outputHeader.add(new CSVFile.CSVField("TL " + head));
        outputHeader.add(new CSVFile.CSVField("TL BGRHallGroup"));
        for (CSVFile.CSVField head: model.getStudentFields())
            outputHeader.add(new CSVFile.CSVField(head));
        outputHeader.add(new CSVFile.CSVField("BGRHallGroup"));
        outputHeader.add(new CSVFile.CSVField("OriginalHallGroup"));
        output.setHeader(outputHeader);
        for (Team t: model.getTeams()) {
            Team.Context cx = t.getContext(solution.getAssignment());
            if (cx.getStudents().isEmpty()) {
                List<CSVFile.CSVField> line = new ArrayList<CSVFile.CSVField>();
                for (CSVFile.CSVField head: model.getTeamFields())
                    line.add(new CSVFile.CSVField(t.getTeamLead().getProperty(head.toString())));
                line.add(new CSVFile.CSVField(t.getTeamLead().getProperty("BGRHallGroup")));
                output.addLine(line);
            } else {
                int idx = 0;
                for (Student s: cx.getStudents()) {
                    List<CSVFile.CSVField> line = new ArrayList<CSVFile.CSVField>();
                    if (idx == 0) {
                        for (CSVFile.CSVField head: model.getTeamFields())
                            line.add(new CSVFile.CSVField(t.getTeamLead().getProperty(head.toString())));
                        line.add(new CSVFile.CSVField(t.getTeamLead().getProperty("BGRHallGroup")));
                    } else {
                        for (int i = 0; i < model.getTeamFields().size() + 1; i++)
                            line.add(new CSVFile.CSVField(""));
                    }
                    for (CSVFile.CSVField head: model.getStudentFields())
                        line.add(new CSVFile.CSVField(s.getProperty(head.toString())));
                    line.add(new CSVFile.CSVField(t.getTeamLead().getProperty("BGRHallGroup")));
                    line.add(new CSVFile.CSVField(s.getProperty("BGRHallGroup")));
                    idx++;
                    output.addLine(line);
                }
            }
        }
        output.save(new File(config.getProperty("Output.Assignments")));
        
        output = new CSVFile();
        outputHeader = new ArrayList<CSVFile.CSVField>();
        for (CSVFile.CSVField head: model.getStudentFields())
            outputHeader.add(new CSVFile.CSVField(head));
        outputHeader.add(new CSVFile.CSVField("BGRHallGroup"));
        outputHeader.add(new CSVFile.CSVField("OriginalHallGroup"));
        output.setHeader(outputHeader);
        for (Student s: model.variables()) {
            List<CSVFile.CSVField> line = new ArrayList<CSVFile.CSVField>();
            for (CSVFile.CSVField head: model.getStudentFields())
                line.add(new CSVFile.CSVField(s.getProperty(head.toString())));
            TeamAssignment ta = solution.getAssignment().getValue(s);
            line.add(new CSVFile.CSVField(ta == null ? s.getProperty("BGRHallGroup") : ta.getTeam().getTeamLead().getProperty("BGRHallGroup")));
            line.add(new CSVFile.CSVField(s.getProperty("BGRHallGroup")));
            output.addLine(line);
        }
        output.save(new File(config.getProperty("Output.Leads")));
    }

}
