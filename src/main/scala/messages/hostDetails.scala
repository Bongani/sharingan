package messages

import java.net.InetAddress

class hostDetails(hostname: String, IPAddress: InetAddress, topName: String, rank: Integer) {
  def name = hostname;
  def ipAdd = IPAddress;
  def topicName = topName;
  def topicRank = rank; //rank of the host to a specific topic

}