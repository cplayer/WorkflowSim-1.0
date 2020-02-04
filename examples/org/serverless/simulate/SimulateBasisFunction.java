package org.serverless.simulate;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.workflowsim.CondorVM;
import org.workflowsim.Job;
import org.workflowsim.Task;
import org.workflowsim.WorkflowDatacenter;
import org.workflowsim.utils.Parameters;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * @author cplayer on 2020/2/1 5:21 下午.
 * @version 1.0
 */

public class SimulateBasisFunction {
    protected static WorkflowDatacenter createDatacenter (String name) {

        // 用以下步骤来创建数据中心：
        // 1. 创建一个列表来存放一个或者多个机器
        List<Host> hostList = new ArrayList<>();

        // 2. 一台机器包含了一个或者多个PE（Process Element in CloudSim）或者CPU/核心。
        //    因此，在创建一个机器之前，需要创建一个列表来存放这些PE。
        for (int i = 1; i <= 20; i++) {
            List<Pe> peList1 = new ArrayList<>();
            int mips = 2000;
            // 3. 创建PE且将其加入列表
            // 对于一个四核机器，一个包含4个PE的列表是必须的：
            // need to store Pe id and MIPS Rating
            // 需要存放Pe的id和MIPS等级
            peList1.add(new Pe(0, new PeProvisionerSimple(mips)));
            peList1.add(new Pe(1, new PeProvisionerSimple(mips)));

            int hostId = 0;
            // Host内存（MB）
            int ram = 2048;
            // Host存储空间
            long storage = 1000000;
            int bw = 10000;
            hostList.add(
                    new Host(
                            hostId,
                            new RamProvisionerSimple(ram),
                            new BwProvisionerSimple(bw),
                            storage,
                            peList1,
                            new VmSchedulerTimeShared(peList1)
                    )
            ); // This is our first machine
            //hostId++;
        }

        // 4. 创建一个数据中心特征（DatacenterCharacteristics）对象，其存储的是一个数据中心的特征：
        //    架构，OS，机器列表，
        //    分配策略：时间/空间分享，
        //    时区以及其价格（G$/Pe 时间单位）。
        // 系统架构
        String arch = "x86";
        // 操作系统
        String os = "Linux";
        String vmm = "Xen";
        // 此资源所处的时区
        double time_zone = 10.0;
        // 在此资源上使用处理器的开销
        double cost = 3.0;
        // 在此资源上使用内存的开销
        double costPerMem = 0.05;
        // 在此资源上使用存储器的开销
        double costPerStorage = 0.1;
        // 在此资源上使用带宽的开销
        double costPerBw = 0.1;
        // we are not adding SAN devices by now（SAN = Storage Area Network，存储区域网络）
        LinkedList<Storage> storageList = new LinkedList<>();
        WorkflowDatacenter datacenter = null;

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        // 5. Finally, we need to create a storage object.
        /**
         * The bandwidth within a data center in MB/s.
         */
        // 5. 最后，我们需要创建一个存储对象。
        // the number comes from the futuregrid site, you can specify your bw
        int maxTransferRate = 15;

        try {
            // 在这里设置带宽为15MB/s
            HarddriveStorage s1 = new HarddriveStorage(name, 1e12);
            s1.setMaxTransferRate(maxTransferRate);
            storageList.add(s1);
            datacenter = new WorkflowDatacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return datacenter;
    }


    protected static List<CondorVM> createVM (int userId, int vms) {

        // 创建一个用以存放VM的容器。
        // 这个列表之后会被传给broker
        LinkedList<CondorVM> list = new LinkedList<>();

        // VM参数
        // 镜像大小（MB）
        long size = 10000;
        // VM内存（MB）
        int ram = 512;
        int mips = 1000;
        long bw = 1000;
        // cpu数量
        int pesNumber = 1;
        // VMM名称（Virtual Machine Monitor，虚拟机器监视器）
        String vmm = "Xen";

        // 创建VM
        CondorVM[] vm = new CondorVM[vms];
        // 随机带宽
        Random bwRandom = new Random(System.currentTimeMillis());
        for (int i = 0; i < vms; i++) {
            double ratio = bwRandom.nextDouble();
            vm[i] = new CondorVM(i, userId, mips * ratio, pesNumber, ram, (long) (bw * ratio), size, vmm, new CloudletSchedulerSpaceShared());
            list.add(vm[i]);
        }
        return list;
    }

    /**
     * Prints the job objects
     *
     * @param list
     *         list of jobs
     */
    protected static void printJobList (List<Job> list) {
        String indent = "\t";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        // Log.printLine("Job ID" + indent + "Task ID" + indent + "STATUS" + indent
        //         + "Data center ID" + indent + "VM ID" + indent + indent
        //         + "Time" + indent + "Start Time" + indent + "Finish Time" + indent + "Depth");
        Log.printLine(String.format("Job ID%sTask ID%sSTATUS%sData center ID%sVM ID%sTime%sStart Time%sFinish Time%sDepth",
                indent,
                indent,
                indent,
                indent,
                indent,
                indent,
                indent,
                indent));
        DecimalFormat dft = new DecimalFormat("###.##");
        for (Job job : list) {
            Log.print(String.format("%d%s", job.getCloudletId(), indent));
            // Log.print(indent + job.getCloudletId() + indent);
            if (job.getClassType() == Parameters.ClassType.STAGE_IN.value) {
                Log.print("Stage-in");
            }
            for (Task task : job.getTaskList()) {
                Log.print(task.getCloudletId() + ",");
            }
            Log.print(indent);

            if (job.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS" + indent);
                Log.printLine(String.format("%d%s%d%s%s%s%s%s%s%s%d",
                        job.getResourceId(),
                        indent,
                        job.getVmId(),
                        indent,
                        dft.format(job.getActualCPUTime()),
                        indent,
                        dft.format(job.getExecStartTime()),
                        indent,
                        dft.format(job.getFinishTime()),
                        indent,
                        job.getDepth()));
                // Log.printLine(indent + indent + job.getResourceId() + indent + indent + indent + job.getVmId()
                //         + indent + indent + indent + dft.format(job.getActualCPUTime())
                //         + indent + indent + dft.format(job.getExecStartTime()) + indent + indent + indent
                //         + dft.format(job.getFinishTime()) + indent + indent + indent + job.getDepth());
            } else if (job.getCloudletStatus() == Cloudlet.FAILED) {
                Log.print("FAILED" + indent);
                Log.printLine(String.format("%d%s%d%s%d%s%d%s%d%s%d",
                        job.getResourceId(),
                        indent,
                        job.getVmId(),
                        indent,
                        dft.format(job.getActualCPUTime()),
                        indent,
                        dft.format(job.getExecStartTime()),
                        indent,
                        dft.format(job.getFinishTime()),
                        indent,
                        job.getDepth()));
                // Log.printLine(indent + indent + job.getResourceId() + indent + indent + indent + job.getVmId()
                //         + indent + indent + indent + dft.format(job.getActualCPUTime())
                //         + indent + indent + dft.format(job.getExecStartTime()) + indent + indent + indent
                //         + dft.format(job.getFinishTime()) + indent + indent + indent + job.getDepth());
            }
        }
    }
}
