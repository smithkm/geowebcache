package org.geowebcache.rest.seed;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.apache.commons.lang.ArrayUtils;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.seed.GWCTask.STATE;
import org.geowebcache.seed.GWCTask.TYPE;
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
    
    protected abstract class Column {
        public Column(String header) {
            this.header=header;
        }
        
        final private String header;
        public String getHeader(){
            return header;
        }
        
        public abstract String getField(JobStatus job, TaskStatus task);
    }

    protected final Column id = new Column("ID"){
        @Override
        public String getField(JobStatus job, TaskStatus task) {
            return Long.toString(task.getTaskId());
        }
    };
    protected final Column layer = new Column("Layer"){
        @Override
        public String getField(JobStatus job, TaskStatus task) {
            return job.getLayerName();
        }
    };
    protected final Column state = new Column("State"){
        @Override
        public String getField(JobStatus job, TaskStatus task) {
            return task.getState().toString();
        }
    };
    protected final Column type  = new Column("Type"){
        @Override
        public String getField(JobStatus job, TaskStatus task) {
            return job.getType().toString();
        }
    };
    protected final Column total_tiles  = new Column("Estimated # of tiles"){
        @Override
        public String getField(JobStatus job, TaskStatus task) {
            return Long.toString(task.getTilesTotal());
        }
    };
    protected final Column cmplt_tiles  = new Column("Tiles Completed"){
        @Override
        public String getField(JobStatus job, TaskStatus task) {
            return Long.toString(task.getTilesDone());
        }
    };
    protected final Column elapsed_time  = new Column("Time Elapsed"){
        @Override
        public String getField(JobStatus job, TaskStatus task) {
            return SeedFormRestlet.toTimeString(task.getTimeSpent(), task.getTilesDone(), task.getTilesTotal());
        }
    };
    protected final Column remaining_time  = new Column("Time Remaining"){
        @Override
        public String getField(JobStatus job, TaskStatus task) {
            return SeedFormRestlet.toTimeString(task.getTimeRemaining(), task.getTilesDone(), task.getTilesTotal());
        }
    };
    protected final Column job_tasks  = new Column("Tasks"){
        @Override
        public String getField(JobStatus job, TaskStatus task) {
            return Long.toString(job.getThreadCount());
        }
    };
    protected final Column kill  = new Column(null){
        @Override
        public String getField(JobStatus job, TaskStatus task) {
            boolean killable = task.getState().isStopped();
            killable |= job.getType()!=TYPE.TRUNCATE;
            String ret = "<form form id=\"kill\" action=\"./"
                    + tl.getName()
                    + "\" method=\"post\">"
                    + "<input type=\"hidden\" name=\"kill_thread\"  value=\"1\" />"
                    + "<input type=\"hidden\" name=\"thread_id\"  value=\""
                    + task.getTaskId()
                    + "\" />"
                    + "<span><input "
                    + (killable ? "" : "disabled=\"disabled\"" )
                    +" style=\"padding: 0; margin-bottom: -12px; border: 1;\" type=\"submit\" value=\"Kill Task\"></span>"
                    + "</form>";
            return ret;
        }
    };
    
    protected Column[] firstColumns = {id};
    protected Column[] midColumns = {layer, state, type, total_tiles, cmplt_tiles, elapsed_time, remaining_time};
    protected Column[] lastColumns = {kill};
   
    protected int getColumnCount(){
        return firstColumns.length+midColumns.length+lastColumns.length;
    }
    
    protected void head() throws IOException {
        doc.append("<thead>\n");
        doc.append("  <tr>");
        
        columnHeaders();
        
        doc.append("  </tr>");
        doc.append("</thead>\n");
    }
    
    protected void columnHeaders() throws IOException {
        
        for(Column col: firstColumns){
            columnHeader(col.getHeader());
        }
        for(Column col: midColumns){
            columnHeader(col.getHeader());
        }
        for(Column col: lastColumns){
            columnHeader(col.getHeader());
        }
   }
    
    protected void columnHeader(String title) throws IOException {
        if(title!=null){
            doc.append("<th scope=\"col\">").append(title).append("</th>");
        } else {
            doc.append("<th scope=\"col\"/>");
        }
    }
    protected void cell(String data, String scope) throws IOException {
        if(data!=null){
            if(scope!=null){
                doc.append("<td scope=\"").append(scope).append("\">");
            } else {
                doc.append("<td>");
            }
            doc.append(data);
            doc.append("</td>");
        } else {
            if(scope!=null){
                doc.append("<td scope=\"").append(scope).append("\"/>");
            } else {
                doc.append("<td/>");
            }
        }
    }
    
    protected void jobBody(JobStatus job) throws IOException {
        doc.append("  <tbody>\n");
        
        jobRow(job);
        for(TaskStatus task: job.getTaskStatuses()){
            taskRow(task, job);
        }
        
        doc.append("  </tbody>\n");
    }
    
    protected void jobRow(JobStatus job) throws IOException {
        doc.append("    <tr class=\"job\">");
        doc.append("<th colspan=\"").append(Integer.toString(getColumnCount()-1)).append("\" scope=\"rowgroup\">Job ");
        doc.append(Long.toString(job.getJobId()));
        doc.append("</th>");
        doc.append("<td>");
        
        boolean killable = job.getState().isStopped();
        killable |= job.getType()!=TYPE.TRUNCATE;
        
        doc.append("<form form id=\"kill\" action=\"./"
                + tl.getName()
                + "\" method=\"post\">"
                + "<input type=\"hidden\" name=\"kill_job\"  value=\"1\" />"
                + "<input type=\"hidden\" name=\"job_id\"  value=\""
                + job.getJobId()
                + "\" />"
                + "<span><input "
                + (killable ? "" : "disabled=\"disabled\"" )
                +"style=\"padding: 0; margin-bottom: -12px; border: 1;\" type=\"submit\" value=\"Kill Job\"></span>"
                + "</form>");
        doc.append("</tr>\n");
    }

    int taskRowCounter = 0;
    protected void taskRow(TaskStatus task, JobStatus job) throws IOException {
        doc.append("<tr class=\"task ").append(taskRowCounter%2==0?"even":"odd").append("\">");
        for(Column col: firstColumns){
            cell(col.getField(job, task), "row");
        }
        for(Column col: midColumns){
            cell(col.getField(job, task), null);
        }
        for(Column col: lastColumns){
            cell(col.getField(job, task), "row");
        }

        doc.append("</tr>\n");
        
        taskRowCounter++;
    }

    protected void empty() throws IOException {
        doc.append("  <tbody><tr class=\"listEmpty\"><td colspan=\"").append(Integer.toString(getColumnCount()-1)).append("\">No running jobs</td></tr></tbody>\n");
    }
    
    public void table(Collection<JobStatus> jobs, String id, String caption) throws IOException {
        if(id!=null){
            doc.append("<table class=\"jobList\" id=\"").append(id).append("\">\n");
        } else {
            doc.append("<table class=\"jobList\">\n");
        }
        if(caption!=null){
            doc.append("  <caption>").append(caption).append("</caption>\n");
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
        doc.append("</table>\n");
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
