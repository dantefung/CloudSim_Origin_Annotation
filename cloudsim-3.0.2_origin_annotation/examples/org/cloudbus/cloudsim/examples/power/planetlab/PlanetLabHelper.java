package org.cloudbus.cloudsim.examples.power.planetlab;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelNull;
import org.cloudbus.cloudsim.UtilizationModelPlanetLabInMemory;
import org.cloudbus.cloudsim.examples.power.Constants;

/**
 * A helper class for the running examples for the PlanetLab workload.
 * 
 * If you are using any algorithms, policies or workload included in the power package please cite
 * the following paper:
 * 
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 * 
 * @author Anton Beloglazov
 * @since Jan 5, 2012
 */
public class PlanetLabHelper {

	/**
	 * Creates the cloudlet list planet lab.
	 * 
	 * @param brokerId the broker id
	 * @param inputFolderName the input folder name
	 * @return the list
	 * @throws FileNotFoundException the file not found exception
	 */
	public static List<Cloudlet> createCloudletListPlanetLab(int brokerId, String inputFolderName)
			throws FileNotFoundException {
		List<Cloudlet> list = new ArrayList<Cloudlet>();//实现创建好一个列表，用来存储云任务。

		long fileSize = 300;//文件大小为300
		long outputSize = 300;//输出大小为300
		UtilizationModel utilizationModelNull = new UtilizationModelNull();//The UtilizationModelNull class is a simple model, according to which a Cloudlet always require zero capacity.

		File inputFolder = new File(inputFolderName);//创建File类对象，传入完整路径:workload/planetlab/888。
		File[] files = inputFolder.listFiles();//返回一个 File 对象数组，每个数组元素抽象路径名对应目录中的每个文件或目录。显然，此时workload/planetlab/888目录下只有一个文件。

		for (int i = 0; i < files.length; i++) {//根据workload/panetlab/888目录下的文件数来创建云任务.
			Cloudlet cloudlet = null;
			try {
				cloudlet = new Cloudlet(
						i,//云任务的ID号。
						Constants.CLOUDLET_LENGTH,//任务长度
						Constants.CLOUDLET_PES,//任务需要的cpu处理单元个数。
						fileSize,//文件大小为300.
						outputSize,//输出大小为300.
						new UtilizationModelPlanetLabInMemory(
								files[i].getAbsolutePath(),//文件的绝对路径。
								Constants.SCHEDULING_INTERVAL/*调度间隔*/), utilizationModelNull, utilizationModelNull);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
			cloudlet.setUserId(brokerId);//云任务与数据中心代理建立联系。
			cloudlet.setVmId(i);//将云任务与虚拟机绑定在一起。
			list.add(cloudlet);
		}

		return list;
	}

}
