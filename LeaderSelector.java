package com.web.util;

import java.util.Collections;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class LeaderSelector implements Watcher, ILeaderCallback{

	private ZooKeeper zk;
	private String zkConf="192.168.1.15:2181";
	private int sessionTimeout = 30000;
	private String root="/selector";
	private String name="leader";
	
	private String me="";
	private ILeaderCallback callback;
	
	
	public LeaderSelector(String config,String root, String name, ILeaderCallback callback) throws Exception{
		this.zkConf=config;
		this.root=root;
		this.name=name;
		this.callback=callback;
		init();
	}
	
	public LeaderSelector() throws Exception{
		callback=this;
		init();
	}
	
	private void init() throws Exception{
		zk = new ZooKeeper(zkConf, sessionTimeout, this);
		
		Stat basestat = zk.exists(root, false);
		if(basestat == null){
			// 创建根节点
			zk.create(root, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT); 
		}
		createMe();
	}
	
	private String makeFullPath(String root, String nodeName){
		if (root.endsWith("/")){
			return root+nodeName;
		}else{
			return root+"/"+nodeName;
		}
	}
	
	private void createMe() throws Exception{
		me=zk.create(makeFullPath(root,name), new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
		int index=me.lastIndexOf('/');
		me=me.substring(index<=0?0:index+1);
		System.out.println("I'm created with name '"+me+"'");
	}
	
	private List<String> getCandidates() throws KeeperException, InterruptedException{
		//try {
			List<String> seqs = zk.getChildren(root, false);
			Collections.sort(seqs);
			return seqs;
		/*} catch (Exception e ) {			
			e.printStackTrace();
			return new ArrayList<String>(0);
		} 	*/	
	}
	
	private boolean canBeLeader() throws KeeperException, InterruptedException {
		List<String> candidates=getCandidates();
		String prevCandidate=null;
		for (String candidate: candidates){
			//System.out.println(candidate);
			if (me.equalsIgnoreCase(candidate)){
				if (prevCandidate==null){
					if (callback!=null){
						callback.leaderCallback(this);
					}
					return true;
				}else{
					//zk.getData(root+"/"+prevCandidate, this, null);
					Stat stat=zk.exists(makeFullPath(root,prevCandidate), this);
					if (stat==null){ //node not exists, retry again. this happens if prevCanidate quit after the time call getCandidates 
						return canBeLeader();
					}
					return false;
				}
			}
			prevCandidate=candidate;
		}
		throw new RuntimeException("I'm not in candidates, it's impossible!");
		
	}
	
	public boolean wantToBeLeaderNowait() throws KeeperException, InterruptedException{
		boolean result = canBeLeader();
		return result;
	}
	
	public void waitToBeLeader() throws Exception{
		while(!wantToBeLeaderNowait()){
			System.out.println("I'm waiting to be leader");
			synchronized(this){
				wait();
			}
		}
		System.out.println("waitToBeLeader success!");
	}
	
	
	public static void main(String[] args) throws Exception{
		LeaderSelector leaderSelector=new LeaderSelector();
		//while (true){
			if (leaderSelector.wantToBeLeaderNowait()){
				System.out.println("I'm leader now");
			}else{
				System.out.println("I'm not leader");
			}
			Thread.sleep(30000);
			
			leaderSelector.waitToBeLeader();
			
			Thread.sleep(10000);
			
			System.out.println("finished!");
		//}

	}

	public void process(WatchedEvent event) {
		System.out.print("event received:");
		System.out.print("watcher="+this.getClass().getName());
		System.out.print("| path="+event.getPath());
		System.out.println("| eventType="+event.getType().name());
		if (event.getType()==EventType.NodeDeleted){
			if (event.getPath().startsWith(makeFullPath(root,name))){
				synchronized(this){
					notifyAll();
				}
			}else{
				System.err.println("node should not be created in this path");
			}
		}
	}

	public void leaderCallback(Object context) {
		System.out.println("callback: I'm leader now");		
	}

}
