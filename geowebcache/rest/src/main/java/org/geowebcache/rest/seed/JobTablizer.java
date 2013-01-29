package org.geowebcache.rest.seed;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Locale;

import org.geowebcache.layer.TileLayer;
import org.geowebcache.seed.GWCTask.STATE;
import org.geowebcache.seed.JobStatus;
import org.geowebcache.seed.TaskStatus;

/**
 * Produces an HTML table from a collection of JobStatus objects
 * @author smithkm
 *
 */
public class JobTablizer {
    protected final Appendable doc;
    protected final TileLayer tl;
    
    

    public JobTablizer(Appendable doc, TileLayer tl) {
        super();
        this.doc = doc;
        this.tl = tl;
    }

    protected void head() throws IOException {
        doc.append("<thead>");
        doc.append("<tr>");
        
        columnHeaders();
        
        doc.append("</tr>");
        doc.append("</thead>");
    }
    //doc.append("<thead><tr><th style=\"padding-right:20px;\">Id</th><th style=\"padding-right:20px;\">Layer</th><th style=\"padding-right:20px;\">Status</th><th style=\"padding-right:20px;\">Type</th><th>Estimated # of tiles</th>"
    //        + "<th style=\"padding-right:20px;\">Tiles completed</th><th style=\"padding-right:20px;\">Time elapsed</th><th>Time remaining</th><th>Tasks</th><th>&nbsp;</th>");
    
    public static final String[] HEADERS = {"ID", "Layer", "Status", "Type", "Estimated # of tiles", "Tiles completed", "Time elapsed", "Time remaining", "Tasks", null};
    protected void columnHeaders() throws IOException {
        
        for(String title: HEADERS){
            columnHeader(title);
        }
    }
    
    protected void columnHeader(String title) throws IOException {
        if(title!=null){
            doc.append("<th scope=\"col\">").append(title).append("</th>");
        } else {
            doc.append("<th/>");
        }
    }
    
    protected void jobBody(JobStatus job) throws IOException {
        doc.append("<tbody>");
        
        jobRow(job);
        for(TaskStatus task: job.getTaskStatuses()){
            taskRow(task, job);
        }
        
        doc.append("</tbody>");
    }
    
    protected void jobRow(JobStatus job) throws IOException {
        doc.append("<tr class=\"job\">");
        doc.append("<th colspan=\"").append(Integer.toString(HEADERS.length-1)).append("\" scope=\"rowgroup\">Job ");
        doc.append(Long.toString(job.getJobId()));
        doc.append("</th>");
        doc.append("<td><s>Kill Job</s></td>"); // TODO
        doc.append("</tr>");
    }

    int taskRowCounter = 0;
    protected void taskRow(TaskStatus task, JobStatus job) throws IOException {
        final long spent = task.getTimeSpent();
        final long remining = task.getTimeRemaining();
        final long tilesDone = task.getTilesDone();
        final long tilesTotal = task.getTilesTotal();

        NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
        nf.setGroupingUsed(true);
        final String tilesTotalStr;
        if (tilesTotal < 0) {
            tilesTotalStr = "Too many to count";
        } else {
            tilesTotalStr = nf.format(tilesTotal);
        }
        final String tilesDoneStr = nf.format(task.getTilesDone());
        final STATE state = task.getState();

        final String status = STATE.UNSET.equals(state) || STATE.READY.equals(state) ? "PENDING"
                : state.toString();

        String timeSpent = SeedFormRestlet.toTimeString(spent, tilesDone, tilesTotal);
        String timeRemaining = SeedFormRestlet.toTimeString(remining, tilesDone, tilesTotal);

        String layerName = tl.getName();
        
        doc.append("<tr class=\"task ").append(taskRowCounter%2==0?"even":"odd").append("\">");
        doc.append("<td scope=\"row\">").append(Long.toString(task.getTaskId())).append("</td>");
        doc.append("<td>");
        if (!layerName.equals(job.getLayerName())) {
            doc.append("<a href=\"./").append(job.getLayerName()).append("\">");
        }
        doc.append(job.getLayerName());
        if (!layerName.equals(job.getLayerName())) {
            doc.append("</a>");
        }
        doc.append("</td>");
        doc.append("<td>").append(status).append("</td>");
        doc.append("<td>").append(job.getType().toString()).append("</td>");
        doc.append("<td>").append(tilesTotalStr).append("</td>");
        doc.append("<td>").append(tilesDoneStr).append("</td>");
        doc.append("<td>").append(timeSpent).append("</td>");
        doc.append("<td>").append(timeRemaining).append("</td>");
        doc.append("<td>").append(Long.toString(job.getThreadCount())).append("</td>");
        doc.append("<td>").append(makeThreadKillForm(task.getTaskId(), tl)).append("</td>");

        doc.append("</tr>");
        
        taskRowCounter++;
    }

    protected void empty() throws IOException {
        doc.append("<tr class=\"listEmpty\"><td colspan=\"*\">None</td></tr>");
    }
    
    public void table(Collection<JobStatus> jobs, String id, String caption) throws IOException {
        if(id!=null){
            doc.append("<table class=\"jobList\" id=\"").append(id).append("\">");
        } else {
            doc.append("<table class=\"jobList\">");
        }
        if(caption!=null){
            doc.append("<caption>").append(caption).append("</caption>");
        }
        head();
        foot();
        if(jobs.isEmpty()){
            empty();
        } else {
            for(JobStatus job: jobs){
                jobBody(job);
            }
        }
        doc.append("</table>");
   }

    protected void foot() throws IOException {
        doc.append("<tfoot/>");
    }
    
    
    protected String makeThreadKillForm(Long key, TileLayer tl) {
        String ret = "<form form id=\"kill\" action=\"./"
                + tl.getName()
                + "\" method=\"post\">"
                + "<input type=\"hidden\" name=\"kill_thread\"  value=\"1\" />"
                + "<input type=\"hidden\" name=\"thread_id\"  value=\""
                + key
                + "\" />"
                + "<span><input style=\"padding: 0; margin-bottom: -12px; border: 1;\" type=\"submit\" value=\"Kill Task\"></span>"
                + "</form>";

        return ret;
    }

}
