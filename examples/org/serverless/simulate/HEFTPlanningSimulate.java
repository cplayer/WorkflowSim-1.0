package org.serverless.simulate;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.workflowsim.*;
import org.workflowsim.utils.ClusteringParameters;
import org.workflowsim.utils.OverheadParameters;
import org.workflowsim.utils.Parameters;
import org.workflowsim.utils.ReplicaCatalog;

import java.io.File;
import java.util.Calendar;
import java.util.List;

import static org.serverless.simulate.SimulateBasisFunction.*;

/**
 * @author cplayer on 2020/2/1 5:06 下午.
 * @version 1.0
 */

public class HEFTPlanningSimulate {
    public static void main (String[] args) {
        try {
            // 虚拟机数量
            int vmNum = 10;
            // 工作流文件路径
            String daxPath = DaxPathConfigure.Montage_25;
            // 检查是否存在对应文件
            File daxFile = new File(daxPath);
            if (!daxFile.exists()) {
                Log.printLine("Warning: Please replace daxPath with the physical path in your working environment!");
                return;
            }

            // 根据例子里所说，若要用HEFT规划算法，则调度算法必须采用静态调度以使得调度器不会覆盖计划器所产生的结果
            Parameters.SchedulingAlgorithm sch_method = Parameters.SchedulingAlgorithm.STATIC;
            Parameters.PlanningAlgorithm pln_method = Parameters.PlanningAlgorithm.HEFT;
            ReplicaCatalog.FileSystem file_system = ReplicaCatalog.FileSystem.LOCAL;

            // Overhead是什么？
            OverheadParameters op = new OverheadParameters(0,
                    null,
                    null,
                    null,
                    null,
                    0);

            // 没有集群
            ClusteringParameters.ClusteringMethod method = ClusteringParameters.ClusteringMethod.NONE;
            ClusteringParameters cp = new ClusteringParameters(0, 0, method, null);

            // 初始化静态参数
            Parameters.init(vmNum, daxPath, null,
                    null, op, cp, sch_method, pln_method,
                    null, 0);
            ReplicaCatalog.init(file_system);

            // 创建任何实体之前
            // 网格用户的个数
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            // 跟踪事件
            boolean trace_flag = false;

            // 初始化CloudSim库
            CloudSim.init(num_user, calendar, trace_flag);

            // 创建数据中心
            WorkflowDatacenter datacenter0 = createDatacenter("Datacenter_0");

            // 创建一个有着一个调度器的工作流计划器
            WorkflowPlanner wfPlanner = new WorkflowPlanner("planner_0", 1);

            // 创建工作流引擎
            WorkflowEngine wfEngine = wfPlanner.getWorkflowEngine();

            // 创建一个VM的列表。
            // 一个VM的userId是控制这台VM的调度器的id
            List<CondorVM> vmlist0 = createVM(wfEngine.getSchedulerId(0), Parameters.getVmNum());

            // 提交vm列表给工作流引擎
            wfEngine.submitVmList(vmlist0, 0);

            // 将数据中心与调度器绑定
            wfEngine.bindSchedulerDatacenter(datacenter0.getId(), 0);

            CloudSim.startSimulation();
            List<Job> outputList0 = wfEngine.getJobsReceivedList();
            CloudSim.stopSimulation();
            printJobList(outputList0);
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }
}
