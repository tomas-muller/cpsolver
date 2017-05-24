package org.cpsolver.bgr;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.cpsolver.exam.MistaTables.Counter;
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
import org.cpsolver.ifs.util.CSVFile.CSVField;

public class TeamBuilder extends Model<Student, TeamAssignment>{
    private static Logger sLog = Logger.getLogger(TeamBuilder.class);
    private List<Team> iTeams = new ArrayList<Team>();
    protected static java.text.DecimalFormat sCounterFormat = new java.text.DecimalFormat("00", new java.text.DecimalFormatSymbols(Locale.US));
    protected List<CSVField> iStudentFields, iTeamFields;
    
    public void addTeam(Team team) {
        team.setModel(this);
        iTeams.add(team);
    }
    
    public List<Team> getTeams() { return iTeams; }
    
    public List<Team> getInternationalTeams() {
        List<Team> teams = new ArrayList<Team>();
        for (Team team: getTeams())
            if (team.getTeamLead().isInternational()) teams.add(team);
        return teams;
    }
    
    public List<Student> getInternationalStudents() {
        List<Student> students = new ArrayList<Student>();
        for (Student student: variables())
            if (student.isInternational()) students.add(student);
        return students;
    }
    
    public int countInternationalTeams() {
        int teams = 0;
        for (Team team: getTeams())
            if (team.getTeamLead().isInternational()) teams ++;
        return teams;
    }
    
    public int countInternationalStudents() {
        int students = 0;
        for (Student student: variables())
            if (student.isInternational()) students ++;
        return students;
    }
    
    protected List<CSVField> getStudentFields() { return iStudentFields; }
    protected List<CSVField> getTeamFields() { return iTeamFields; }
    
    @Override
    public double getTotalValue(Assignment<Student, TeamAssignment> assignment) {
        double ret = 0;
        for (Criterion<Student, TeamAssignment> criterion : getCriteria())
            ret += criterion.getWeightedValue(assignment);
        return ret;
    }

    @Override
    public double getTotalValue(Assignment<Student, TeamAssignment> assignment, Collection<Student> variables) {
        double ret = 0;
        for (Criterion<Student, TeamAssignment> criterion : getCriteria())
            ret += criterion.getWeightedValue(assignment, variables);
        return ret;
    }
    
    @Override
    public Map<String, String> getExtendedInfo(Assignment<Student, TeamAssignment> assignment) {
        Map<String, String> ret = super.getExtendedInfo(assignment);
        Map<Integer, Integer> size2count = new HashMap<Integer, Integer>();
        Map<Integer, Integer> inter2count = new HashMap<Integer, Integer>();
        Map<Integer, Map<String, Integer>> inter2hist = new HashMap<Integer, Map<String, Integer>>();
        Map<Integer, Map<String, Integer>> size2hist = new HashMap<Integer, Map<String, Integer>>();
        Counter tc = new Counter(), ic = new Counter();
        for (Team t: getTeams()) {
            Team.Context cx = t.getContext(assignment);
            Integer count = size2count.get(cx.getStudents().size());
            size2count.put(cx.getStudents().size(), 1 + (count == null ? 0 : count.intValue()));
            if (t.getTeamLead().isInternational() || !cx.getInternationalStudents().isEmpty()) {
                Integer interns = inter2count.get(cx.getInternationalStudents().size());
                inter2count.put(cx.getInternationalStudents().size(), 1 + (interns == null ? 0 : interns.intValue()));
                Map<String, Integer> hist = inter2hist.get(cx.getInternationalStudents().size());
                if (hist == null) {
                    hist = new HashMap<String, Integer>();
                    inter2hist.put(cx.getInternationalStudents().size(), hist);
                }
                Integer hinterns = hist.get(t.getTeamLead().getProperty("BGRHallGroup"));
                hist.put(t.getTeamLead().getProperty("BGRHallGroup"), 1 + (hinterns == null ? 0 : hinterns.intValue()));
            }
            tc.inc(cx.getStudents().size());
            if (t.getTeamLead().isInternational())
                ic.inc(cx.getInternationalStudents().size());
            Map<String, Integer> hist = size2hist.get(cx.getStudents().size());
            if (hist == null) {
                hist = new HashMap<String, Integer>();
                size2hist.put(cx.getStudents().size(), hist);
            }
            Integer hcount = hist.get(t.getTeamLead().getProperty("BGRHallGroup"));
            hist.put(t.getTeamLead().getProperty("BGRHallGroup"), 1 + (hcount == null ? 0 : hcount.intValue()));
        }
        for (Map.Entry<Integer, Integer> e: size2count.entrySet())
            ret.put("Team of " + sCounterFormat.format(e.getKey()) + " students", e.getValue().toString() + " " + size2hist.get(e.getKey()));
        for (Map.Entry<Integer, Integer> e: inter2count.entrySet())
            ret.put("Team of international " + sCounterFormat.format(e.getKey()) + " students", e.getValue().toString()  + " " + inter2hist.get(e.getKey()));
        ret.put("Average Team Size", sDoubleFormat.format(tc.avg()) + " +/- " + sDoubleFormat.format(tc.rms()) + " [" + ((int)tc.min()) + ".." + ((int)tc.max()) + "]");
        ret.put("Average International Team Size", sDoubleFormat.format(ic.avg()) + " +/- " + sDoubleFormat.format(ic.rms())+ " [" + ((int)ic.min()) + ".." + ((int)ic.max()) + "]");
        return ret;
    }
    
    protected static void fixBGRHallGroup(Student student) {
        String rh = student.getProperty("Residence Hall");
        if (rh == null) rh = student.getProperty("ResHall");
        String rhg = student.getProperty("BGRHallGroup");
        if (rh != null && rhg == null) {
            if ("Harrison Hall".equals(rh) || "Hawkins Hall".equals(rh) || "McCutcheon Hall".equals(rh))
                rhg = "McHarrison";
            else if ("Cary Quadrangle".equals(rh) || "Off Campus".equals(rh) || "Owen Hall".equals(rh))
                rhg = "OOC";
            else if ("Shreve Hall".equals(rh) || "Earhart Hall".equals(rh))
                rhg = "Shrevehart";
            else if ("First Street Towers".equals(rh) || "Hilltop Apartments".equals(rh) || "Purdue Village".equals(rh) || "Tarkington Hall".equals(rh) || "Wiley Hall".equals(rh))
                rhg = "TWHop";
            else if ("Honors College Residences".equals(rh) || "Hillenbrand Hall".equals(rh) || "Meredith Hall".equals(rh) || "Third Street Suites".equals(rh) || "Windsor Halls".equals(rh))
                rhg = "WHoMT";
            else {
                sLog.error("Unknown residence hall: " + rh);
                rhg = "OTHER";
            }
            student.setProperty("BGRHallGroup", rhg);
        }
    }
    
    protected String getHallGroup(Student student) {
        String rhg = student.getProperty("BGRHallGroup");
        if (rhg == null) rhg = "";
        return rhg;
    }
    
    protected Map<String, List<Student>> getHallGroup2Students(boolean international) {
        final Map<String, List<Student>> rhg2students = new HashMap<String, List<Student>>();
        for (Student student: variables()) {
            if (international && !student.isInternational()) continue;
            String rhg = getHallGroup(student);
            List<Student> students = rhg2students.get(rhg);
            if (students == null) {
                students = new ArrayList<Student>();
                rhg2students.put(rhg, students);
            }
            students.add(student);
        }
        return rhg2students;
    }
    
    protected Map<String, List<Team>> getHallGroup2Teams(boolean international) {
        Map<String, List<Team>> rhg2teams = new HashMap<String, List<Team>>();
        for (Team team: getTeams()) {
            if (international && !team.getTeamLead().isInternational()) continue;
            String rhg = getHallGroup(team.getTeamLead());
            List<Team> teams = rhg2teams.get(rhg);
            if (teams == null) {
                teams = new ArrayList<Team>();
                rhg2teams.put(rhg, teams);
            }
            teams.add(team);
        }
        return rhg2teams;
    }
    
    protected void loadData(DataProperties config) throws IOException {
        CSVFile studentFile = new CSVFile(new File(config.getProperty("Input.Students")));
        iStudentFields = studentFile.getHeader().getFields();
        for (CSVFile.CSVLine line: studentFile.getLines()) {
            Student student = new Student();
            for (CSVFile.CSVField head: iStudentFields) {
                CSVFile.CSVField field = line.getField(head.toString());
                if (field != null && !field.toString().isEmpty())
                    student.setProperty(head.toString(), field.toString());
            }
            fixBGRHallGroup(student);
            addVariable(student);
        }
        
        CSVFile leadFile = new CSVFile(new File(config.getProperty("Input.Leads")));
        iTeamFields = leadFile.getHeader().getFields();
        for (CSVFile.CSVLine line: leadFile.getLines()) {
            Student student = new Student();
            for (CSVFile.CSVField head: iTeamFields) {
                CSVFile.CSVField field = line.getField(head.toString());
                if (field != null && !field.toString().isEmpty())
                    student.setProperty(head.toString(), field.toString());
            }
            fixBGRHallGroup(student);
            Team team = new Team(student);
            addTeam(team);
        }
    }
    
    public static void main(String[] args) throws IOException {
        DataProperties config = new DataProperties();
        config.load(TeamBuilder.class.getClass().getResourceAsStream("/org/cpsolver/bgr/teams.properties"));
        config.putAll(System.getProperties());
        ToolBox.configureLogging();
        
        TeamBuilder model = new TeamBuilder();
        
        model.loadData(config);
        
        int teamSize = 1 + (int)Math.ceil(((double)model.variables().size()) / model.getTeams().size());
        int internationalTeamSize = 1 + (int)Math.ceil(((double)model.countInternationalStudents()) / model.countInternationalTeams());

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
                System.err.println("- " + students.size() + " students in " + rhg + " (" + teams.size() + " teams, " + sDoubleFormat.format(((double)students.size()) / teams.size()) + " average)");
            else
                System.out.println("- " + (students == null ? "0" : "" + students.size()) + " students in " + rhg + " (" + teams.size() + " teams, " + sDoubleFormat.format(((double)(students == null ? 0 : students.size())) / teams.size()) + " average)");
            out.println("- " + (students == null ? "0" : "" + students.size()) + " students in " + rhg + " (" + teams.size() + " teams, " + sDoubleFormat.format(((double)(students == null ? 0 : students.size())) / teams.size()) + " average)");
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
                System.err.println("- " + students.size() + " international students in " + rhg + " (" + teams.size() + " teams, " + sDoubleFormat.format(((double)students.size()) / teams.size()) + " average)");
            else
                System.out.println("- " + (students == null ? "0" : "" + students.size()) + " international students in " + rhg + " (" + teams.size() + " teams, " + sDoubleFormat.format(((double)(students == null ? 0 : students.size())) / teams.size()) + " average)");
            out.println("- " + (students == null ? "0" : "" + students.size()) + " international students in " + rhg + " (" + teams.size() + " teams, " + sDoubleFormat.format(((double)(students == null ? 0 : students.size())) / teams.size()) + " average)");
        }
        out.println(); out.flush();
                
        model.addGlobalConstraint(new InternationalTeamLead());
        model.addGlobalConstraint(new RequiredFeature(RequiredFeature.Mode.SOFT_TEAMS, "BGRHallGroup"));
        model.addGlobalConstraint(new TeamSize(teamSize));
        if (internationalTeamSize < teamSize)
            model.addGlobalConstraint(new InternationalTeamSize(internationalTeamSize));
        
        model.addCriterion(new Feature("Gender"){});
        model.addCriterion(new Feature("Ethnicity"){});
        model.addCriterion(new Feature("BGRi"){});
        model.addCriterion(new Feature("Residence Hall", "ResHall"){});
        model.addCriterion(new LargeTeam(false, (int)Math.ceil(((double)model.variables().size()) / model.getTeams().size())));
        model.addCriterion(new LargeTeam(true, (int)Math.ceil(((double)model.countInternationalStudents()) / model.countInternationalTeams())));
        model.addCriterion(new SmallTeam(false, (int)Math.floor(((double)model.variables().size()) / model.getTeams().size())));
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
        
        /*
        final Map<String, List<Student>> rhg2interns = model.getHallGroup2Students(true);
        final Map<String, List<Team>> rhg2iteams = model.getHallGroup2Teams(true);
        final Map<String, List<Student>> rhg2students = model.getHallGroup2Students(false);
        final Map<String, List<Team>> rhg2teams = model.getHallGroup2Teams(false);

        for (Map.Entry<String, List<Student>> e: rhg2interns.entrySet()) {
            String rhg = e.getKey();
            List<Student> students = e.getValue();
            List<Team> teams = rhg2iteams.get(rhg);
            System.out.println(rhg + ": " + students.size() + " students and " + (teams == null ? 0 : teams.size()) + " teams");
        }
        
        for (Map.Entry<String, List<Team>> e: rhg2teams.entrySet()) {
            String rhg = e.getKey();
            List<Team> teams = e.getValue();
            List<Team> iteams = rhg2iteams.get(rhg);
            System.out.println(rhg + ": " + teams.size() + " teams and " + (iteams == null ? 0 : iteams.size()) + " international teams");
        }
        
        for (Map.Entry<String, List<Student>> e: rhg2interns.entrySet()) {
            String rhg = e.getKey();
            List<Student> students = e.getValue();
            int nrTeams = (int)Math.ceil(((double)students.size()) / internationalTeamSize);
            List<Team> teams = rhg2iteams.get(rhg);
            if (teams == null) { teams = new ArrayList<Team>(); rhg2iteams.put(rhg, teams); }
            if (teams.size() < nrTeams) {
                for (Iterator<Team> i = rhg2iteams.get("").iterator(); teams.size() < nrTeams && i.hasNext(); ) {
                    Team t = i.next();
                    rhg2teams.get(rhg).add(t);
                    rhg2teams.get("").remove(t);
                    teams.add(t); i.remove();
                }
            } else while (teams.size() > nrTeams) {
                Team t = teams.get(0);
                teams.remove(0);
                t.getTeamLead().setProperty("BGRHallGroup", null);
                rhg2iteams.get("").add(t);
                rhg2teams.get(rhg).remove(t);
                rhg2teams.get("").add(t);
            }
        }
        
        List<String> rhgs = new ArrayList<String>(rhg2interns.keySet());
        Collections.sort(rhgs, new Comparator<String>() {
            @Override
            public int compare(String rhg1, String rhg2) {
                int s1 = rhg2interns.get(rhg1).size();
                int s2 = rhg2interns.get(rhg2).size();
                if (s1 != s2)
                    return s1 > s2 ? -1 : 1;
                return rhg1.compareTo(rhg2);
            }
        });
        List<Team> leftover = rhg2iteams.get("");
        for (int i = 0; i < leftover.size(); i++) {
            String rhg = rhgs.get(i % rhgs.size());
            Team t = leftover.get(i);
            rhg2iteams.get(rhg).add(t);
            rhg2teams.get(rhg).add(t);
            rhg2teams.get("").remove(t);
        }
        
        for (Map.Entry<String, List<Student>> e: rhg2interns.entrySet()) {
            String rhg = e.getKey();
            List<Student> students = e.getValue();
            List<Team> teams = rhg2iteams.get(rhg);
            for (Student student: students) {
                TeamAssignment best = null; double bestVal = 0.0;
                for (Team team: teams) {
                    if (team.getContext(assignment).getStudents().size() >= internationalTeamSize) continue;
                    TeamAssignment ta = new TeamAssignment(student, team);
                    double value = 10000.0 * team.getContext(assignment).getStudents().size() + ta.toDouble(assignment);
                    if (best == null || value < bestVal) { best = ta; bestVal = value; }
                }
                if (best != null) {
                    assignment.assign(solver.currentSolution().getIteration(), best); solver.currentSolution().update(0.0);
                }
            }
        }
        
        sLog.info("Info: " + ToolBox.dict2string(solver.currentSolution().getExtendedInfo(), 2));
        
        for (Map.Entry<String, List<Student>> e: rhg2students.entrySet()) {
            String rhg = e.getKey();
            List<Student> students = e.getValue();
            int nrTeams = (int)Math.ceil(((double)students.size()) / teamSize);
            List<Team> teams = rhg2teams.get(rhg);
            if (teams == null) { teams = new ArrayList<Team>(); rhg2teams.put(rhg, teams); }
            if (teams.size() < nrTeams) {
                for (Iterator<Team> i = rhg2teams.get("").iterator(); teams.size() < nrTeams && i.hasNext(); ) {
                    teams.add(i.next()); i.remove();
                }
            }
        }
        
        rhgs = new ArrayList<String>(rhg2students.keySet());
        Collections.sort(rhgs, new Comparator<String>() {
            @Override
            public int compare(String rhg1, String rhg2) {
                int s1 = rhg2students.get(rhg1).size();
                int s2 = rhg2students.get(rhg2).size();
                if (s1 != s2)
                    return s1 > s2 ? -1 : 1;
                return rhg1.compareTo(rhg2);
            }
        });
        leftover = rhg2teams.get("");
        for (int i = 0; i < leftover.size(); i++) {
            rhg2teams.get(rhgs.get(i % rhgs.size())).add(leftover.get(i));
        }
        
        for (Map.Entry<String, List<Student>> e: rhg2students.entrySet()) {
            String rhg = e.getKey();
            List<Student> students = e.getValue();
            List<Team> teams = rhg2teams.get(rhg);
            for (Student student: students) {
                if (student.getAssignment(assignment) != null) continue;
                TeamAssignment best = null; double bestVal = 0.0;
                for (Team team: teams) {
                    if (team.getContext(assignment).getStudents().size() >= teamSize) continue;
                    TeamAssignment ta = new TeamAssignment(student, team);
                    double value = 10000.0 * team.getContext(assignment).getStudents().size() + ta.toDouble(assignment);
                    if (best == null || value < bestVal) { best = ta; bestVal = value; }
                }
                if (best != null) {
                    assignment.assign(solver.currentSolution().getIteration(), best); solver.currentSolution().update(0.0);
                }
            }
        }
        
        sLog.info("Info: " + ToolBox.dict2string(solver.currentSolution().getExtendedInfo(), 2));
        */

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

        sLog.info("Info: " + ToolBox.dict2string(solution.getExtendedInfo(), 2));
        out.println("Info: " + ToolBox.dict2string(solution.getExtendedInfo(), 2));
        
        String header = "PUID,International,BGRHallGroup";
        for (Criterion<Student, TeamAssignment> criterion: model.getCriteria())
            if (criterion instanceof Feature)
                header += "," + ((Feature)criterion).getKey();
        out.println(header);
        for (Team t: model.getTeams()) {
            Team.Context cx = t.getContext(solution.getAssignment());
            String hline = t.getTeamLead().getProperty("PUID") + "," + t.getTeamLead().isInternational() + "," + t.getTeamLead().getProperty("BGRHallGroup");
            for (Criterion<Student, TeamAssignment> criterion: model.getCriteria())
                if (criterion instanceof Feature) {
                    String value = ((Feature)criterion).getProperty(t.getTeamLead());
                    hline += ",\"" + (value == null ? "" : value) + "\"";
                }
            out.println(hline);
            int idx = 1;
            for (Student s: cx.getStudents()) {
                String line = "  [" + sCounterFormat.format(idx++) + "]  " + s.getProperty("PUID") + "," + s.isInternational() + "," + s.getProperty("BGRHallGroup");
                for (Criterion<Student, TeamAssignment> criterion: model.getCriteria())
                    if (criterion instanceof Feature) {
                        String value = ((Feature)criterion).getProperty(s);
                        line += ",\"" + (value == null ? "" : value) + "\"";
                    }
                out.println(line);
            }
        }
        out.flush(); out.close();
        
        CSVFile output = new CSVFile();
        List<CSVFile.CSVField> outputHeader = new ArrayList<CSVFile.CSVField>();
        for (CSVFile.CSVField head: model.getTeamFields())
            outputHeader.add(new CSVFile.CSVField("TL " + head));
        for (CSVFile.CSVField head: model.getStudentFields())
            outputHeader.add(new CSVFile.CSVField(head));
        outputHeader.add(new CSVFile.CSVField("BGRHallGroup"));
        output.setHeader(outputHeader);
        for (Team t: model.getTeams()) {
            Team.Context cx = t.getContext(solution.getAssignment());
            if (cx.getStudents().isEmpty()) {
                List<CSVFile.CSVField> line = new ArrayList<CSVFile.CSVField>();
                for (CSVFile.CSVField head: model.getTeamFields())
                    line.add(new CSVFile.CSVField(t.getTeamLead().getProperty(head.toString())));
                output.addLine(line);
            } else {
                int idx = 0;
                for (Student s: cx.getStudents()) {
                    List<CSVFile.CSVField> line = new ArrayList<CSVFile.CSVField>();
                    if (idx == 0) {
                        for (CSVFile.CSVField head: model.getTeamFields())
                            line.add(new CSVFile.CSVField(t.getTeamLead().getProperty(head.toString())));
                    } else {
                        for (int i = 0; i < model.getTeamFields().size(); i++)
                            line.add(new CSVFile.CSVField(""));
                    }
                    for (CSVFile.CSVField head: model.getStudentFields())
                        line.add(new CSVFile.CSVField(s.getProperty(head.toString())));
                    line.add(new CSVFile.CSVField(s.getProperty("BGRHallGroup")));
                    idx++;
                    output.addLine(line);
                    if (t.getTeamLead().getProperty("BGRHallGroup") == null)
                        t.getTeamLead().setProperty("BGRHallGroup", s.getProperty("BGRHallGroup"));
                }
            }
        }
        output.save(new File(config.getProperty("Output.Assignments")));
        
        output = new CSVFile();
        outputHeader = new ArrayList<CSVFile.CSVField>();
        for (CSVFile.CSVField head: model.getTeamFields())
            outputHeader.add(new CSVFile.CSVField(head));
        outputHeader.add(new CSVFile.CSVField("BGRHallGroup"));
        output.setHeader(outputHeader);
        for (Team t: model.getTeams()) {
            List<CSVFile.CSVField> line = new ArrayList<CSVFile.CSVField>();
            for (CSVFile.CSVField head: model.getTeamFields())
                line.add(new CSVFile.CSVField(t.getTeamLead().getProperty(head.toString())));
            line.add(new CSVFile.CSVField(t.getTeamLead().getProperty("BGRHallGroup")));
            output.addLine(line);
        }
        output.save(new File(config.getProperty("Output.Leads")));
    }
}
