package EvenTrafficBehaviours;

import jade.core.*;
import jade.core.behaviours.*;
import java.util.concurrent.TimeUnit;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.awt.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import EvenTrafficClasses.*;

public class TrafficManagerBehaviour extends CyclicBehaviour 
{
	
	private MessageTemplate requestMessage;
	private MessageTemplate informMessage;
	private MessageTemplate queryIfMessage;
	private MessageTemplate queryRefMessage;
	private ArrayList<AgentClass> ambulanceAgents;
	private ArrayList<AgentClass> agents;
	private ArrayList<ACLMessage> requestMsgs;
	//zasięg agentów reagujących na karetkę
	private double ambulanceDistance;
	private long startTime;
	private long tempTime;
	private long currentTime;
	private double writeTime;
	private double distanceBetweenAgents;
	
	public TrafficManagerBehaviour(Agent a) 
	{
		super(a);

		requestMessage=MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
		informMessage=MessageTemplate.MatchPerformative(ACLMessage.INFORM);
		queryIfMessage=MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF);
		queryRefMessage=MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF);
		startTime = System.currentTimeMillis();
		ambulanceDistance=10;
		agents=new ArrayList<AgentClass>();
		ambulanceAgents=new ArrayList<AgentClass>();
	}
	//Główna metoda klasy; Wykonuje się w nieskończonej pętli
	public void action()
	{
		tempTime=System.currentTimeMillis();
		currentTime=System.currentTimeMillis();
		while((currentTime-tempTime)<1000)
		{
			//pobieranie wiadomości i sprawdzenie czy trzeba obłużyć karetkę
			agents=ReceiveAgentState(informMessage);
			requestMsgs=GetMessages(requestMessage);
			if(requestMsgs!=null && requestMsgs.size()>0)
			{
				HandleAmbulance(requestMsgs);
			}
			HandleAgentsQueryRef();
			HandleCarsQueryIf();
			currentTime=System.currentTimeMillis();
		}
		System.out.println("TrafficManager lives, agent size ="+agents.size());
		SaveAgentsState();
	}
	
	
	// Wysłanie wiadomości typu request do samochodów, aby zjechali z drogi, a do skrzyżowania, żeby zmieniło odpowiednio światła
	public void HandleAmbulance(ArrayList<ACLMessage> msgs)
	{
		ambulanceAgents=GetAgentsFromMessage(msgs);
		ArrayList<AgentClass> closeAgents;
		for(AgentClass ambulance:ambulanceAgents)
		{
			closeAgents=GetCloseAgents(ambulance.x,ambulance.y,ambulance.direction);
			for(AgentClass closeAgent:closeAgents)
			{
				System.out.println("CLOSE AGENTS:" + closeAgent.name);
				sendRequestMessageToCarAgent(closeAgent,ambulance);
			}
		}
	}
	// Wysłanie odpowiedzi na zapytanie agenta, czy może już jechać po przepuszczeniu karetki
	public void HandleCarsQueryIf()
	{
		ArrayList<ACLMessage> msgs = GetMessages(queryIfMessage);
		ArrayList<AgentClass> queryIfAgents=GetAgentsFromMessage(msgs);
		ArrayList<AgentClass> closeAgents;
		ArrayList<AgentClass> agentWhichShouldntGoBackOnTrack=new ArrayList<AgentClass>();
		if(msgs!=null && msgs.size()>0)
		{
		for(AgentClass ambulance:ambulanceAgents)
		{
			closeAgents=GetCloseAgents(ambulance.x,ambulance.y,ambulance.direction);
			for(AgentClass agent:queryIfAgents)
			{
				if(isListContainAgent(closeAgents,agent))
					agentWhichShouldntGoBackOnTrack.add(agent);
				//System.out.println("QUERY IF AGENTS:" + agent.name);
			}
			for(AgentClass agent:closeAgents)
			{
			
			}
		}
		for(AgentClass agent:queryIfAgents)
		{
			if(isListContainAgent(agentWhichShouldntGoBackOnTrack,agent)==false)
			{
				SendInformIFMessage(agent);
			}
		}
		}
	}
	// Wysyłanie danych o następnym agencie do pytającego agenta. Jeżeli przed agentem nie znajduje się nic: wyślij aktualną pozycje agenta z typem agenta równym "none";
	public void HandleAgentsQueryRef()
	{
		ArrayList<ACLMessage> messages=GetMessages(queryRefMessage);
		ArrayList<AgentClass> carAgents=GetAgentsFromMessage(messages);
		AgentClass tempAgent;

		for(AgentClass carAgent:carAgents)
		{
			distanceBetweenAgents=1000;
			AgentClass requestingAgent=new AgentClass();
			requestingAgent.type="none";
			requestingAgent.x=carAgent.x;
			requestingAgent.y=carAgent.y;
			requestingAgent.direction=carAgent.direction;
			requestingAgent.lightColor=carAgent.lightColor;
			
			for(AgentClass agent:agents)
			{
				if((new String(carAgent.direction).equals(agent.direction)) || (new String(agent.direction).equals("null")))
				{
					tempAgent=GetRelevantAgent(carAgent,agent);
					System.out.println("agent type=" + agent.type +" x= " + agent.x + " y=" + agent.y + " direction " + agent.direction);
					System.out.println("agent type=" + carAgent.type +" x= " + carAgent.x + " y=" + carAgent.y + " direction " + carAgent.direction);
					if(tempAgent!=null)
					{
						System.out.println("Agent set. distanceBetweenAgents = " + distanceBetweenAgents);
						requestingAgent=tempAgent;
					}
				}
			}
			sendMessageToCarAgent(carAgent,requestingAgent);
			
		}	
	}
	
	
	//wysłanie wiadomości typu inform
	public void sendMessageToCarAgent(AgentClass carAgent,AgentClass requestedAgent)
	{
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setContent(requestedAgent.type + " " + requestedAgent.x +" "+requestedAgent.y +" "+requestedAgent.direction + " "+requestedAgent.lightColor);
		msg.addReceiver(carAgent.aid);
		myAgent.send(msg);
	}
	//wysłanie wiadomości typu request. W tym przypadku nie wysyłamy pełnych danych o karetce, wystarczy jedynie kierunek. Wiadomość jest wysyłana zarówno samochodom jak i światłom drogowym.
	public void sendRequestMessageToCarAgent(AgentClass carAgent,AgentClass ambulance)
	{
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		msg.setContent(ambulance.direction);
		msg.addReceiver(carAgent.aid);
		myAgent.send(msg);
	}
	//wysłanie wiadomości typu inform_if. W tym przypadku wysyłamy tylko wiadomość bez zawartości
	public void SendInformIFMessage(AgentClass agent)
	{
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM_IF);
		msg.setContent(" ");
		msg.addReceiver(agent.aid);
		myAgent.send(msg);
	}
	// Sprawdzenie,czy podany agent znajduje się w podanej liście
	public boolean isListContainAgent(ArrayList<AgentClass> agentsList, AgentClass agent)
	{
		for(AgentClass a:agentsList)
		{
			if(new String(a.name).equals(agent.name))
				return true;
		}
		return false;
		
	}
	// Pobieranie agentów znajdujących się w odpowiedniej odległości od karetki i leżących na tym samym odcinku 
	public ArrayList<AgentClass> GetCloseAgents(double x, double y, String direction)
	{
		ArrayList<AgentClass> agentsList=new ArrayList<AgentClass>();
		for(AgentClass agent:agents)
		{
			
			if((new String(direction).equals("north")) &&  AgentsOnTheSameLine(x,y,direction,agent))
		{
			if((Math.abs(agent.y-y)) < ambulanceDistance)
			{
				agentsList.add(agent);
			}
		}
		if((new String(direction).equals("south")) && AgentsOnTheSameLine(x,y,direction,agent))
		{
			if((Math.abs(y - agent.y )) < ambulanceDistance)
			{
				agentsList.add(agent);
			}
		}
		if((new String(direction).equals("east"))  &&  AgentsOnTheSameLine(x,y,direction,agent))
		{
			if((Math.abs(agent.x - x)) < ambulanceDistance)
			{
				agentsList.add(agent);
			}
		}
		if((new String(direction).equals("west")) &&  AgentsOnTheSameLine(x,y,direction,agent))
		{
			if((Math.abs(x- agent.x)) < ambulanceDistance)
			{
				agentsList.add(agent);
			}
		}
	
		}

		return agentsList;
		
	}
	// sprawdzenie czy agenty znajdują się na tym samym odcinku. Pierwszy warunek : Math.abs(agent.y-y))<=1 uwzględnia również agenty, które robią miejsce ambulansowi
	public boolean AgentsOnTheSameLine(double x, double y,String direction, AgentClass agent)
	{
		if((Math.abs(agent.y-y))<=1 && x<=20 && agent.x<=20 && (new String(direction).equals(agent.direction) || new String(agent.direction).equals("null")))
			return true;
		if((Math.abs(agent.y-y))<=1 && x>=20 && x<=40 && agent.x>=20 && agent.x<=40 && (new String(direction).equals(agent.direction)|| new String(agent.direction).equals("null")))
			return true;
		if((Math.abs(agent.y-y))<=1 && x>=40 && agent.x>=40 && (new String(direction).equals(agent.direction) || new String(agent.direction).equals("null")))
			return true;
		if((Math.abs(agent.x-x))<=1 && y<=20 && agent.y<=20 && (new String(direction).equals(agent.direction) || new String(agent.direction).equals("null")))
			return true;
		if((Math.abs(agent.x-x))<=1 && y>=20 && agent.y>=20 && (new String(direction).equals(agent.direction) || new String(agent.direction).equals("null")))
			return true;
		return false;
	}
	
	// Pobieranie informacji o agentach z otrzymanych wiadomości zadanego typu: jeżeli agent już został wcześniej pobrany, uaktualnij stan, w przeciwnym wypadku - dodaj agenta. 
	public ArrayList<AgentClass> ReceiveAgentState(MessageTemplate template)
	{
		ArrayList<ACLMessage> messages = GetMessages(template);
		boolean isNewElement=true;
		for(ACLMessage message:messages)
		{
			for(int i=0;i<agents.size();i++)
			{

				if(new String(agents.get(i).name).equals(message.getSender().getLocalName()))
				{
					agents.set(i,ParseAgent(message));
					
					isNewElement=false;
					break;
				}
			}
			if(isNewElement)
			{
				agents.add(ParseAgent(message));
			}
			isNewElement=true;
		}
		return agents;
	}
	
	// Pobieranie informacji o agentach z otrzymanych wiadomości: jeżeli agent już został wcześniej pobrany, uaktualnij stan, w przeciwnym wypadku - dodaj agenta. 
	public ArrayList<AgentClass> GetAgentsFromMessage(ArrayList<ACLMessage> messages)
	{
		ArrayList<AgentClass> carAgents=new ArrayList<AgentClass>();
		boolean isNewElement=true;
		for(ACLMessage message:messages)
		{
			for(int i=0;i<carAgents.size();i++)
			{

				if(new String(carAgents.get(i).name).equals(message.getSender().getLocalName()))
				{
					carAgents.set(i,ParseAgent(message));
					
					isNewElement=false;
					break;
				}
			}
			if(isNewElement)
			{
				carAgents.add(ParseAgent(message));
			}
			isNewElement=true;
		}
		return carAgents;
	}
	
	//Pobieranie wiadomości o zadanym typie
	public ArrayList<ACLMessage> GetMessages(MessageTemplate template)
	{
		ArrayList<ACLMessage> messages = new ArrayList<ACLMessage>();
		ACLMessage msg = myAgent.receive(template);
		
		while(msg!=null)
		{
			messages.add(msg);
			msg=myAgent.receive(template);
		}
		return messages;
	}

	// Parsowanie agentów z wiadomości
	public AgentClass ParseAgent(ACLMessage message)
	{
		String content=message.getContent();
		String[] stringList = content.split(" ");
		AgentClass agent=new AgentClass();
		agent.type=stringList[0];
		agent.x=Double.parseDouble(stringList[1]);
		agent.y=Double.parseDouble(stringList[2]);
		agent.direction=stringList[3];
		agent.lightColor=stringList[4];
		agent.name=message.getSender().getLocalName();
		agent.aid=message.getSender();
		return agent;
	}

	// zwraca agenta, jeżeli ten jest bliżej, niż ostatni wybrany agent. 
	public AgentClass GetRelevantAgent(AgentClass carAgent,AgentClass agent)
	{
		if(((new String(carAgent.direction).equals("north")) &&  carAgent.x==agent.x))
		{
			if((agent.y >carAgent.y) && (agent.y - carAgent.y)<distanceBetweenAgents)
			{
				if((new String(agent.direction).equals("null")) && (agent.y==carAgent.y))
					return null;
				System.out.println("Why it came here agent.y= " + agent.y + " and agent.y-carAgent.y = " + (agent.y >carAgent.y) );
				distanceBetweenAgents=(agent.y - carAgent.y);
				return agent;
			}
		}
		if(((new String(carAgent.direction).equals("south")) && carAgent.x==agent.x))
		{
			if((agent.y <carAgent.y) && (carAgent.y - agent.y)<distanceBetweenAgents)
			{
				
				if((new String(agent.direction).equals("null")) && (agent.y==carAgent.y))
					return null;
				distanceBetweenAgents=(carAgent.y - agent.y);
				System.out.println("agent.x= " + agent.x + " agent.y= " + agent.y + " distanceBetweenAgents " + distanceBetweenAgents);
				return agent;
			}
		}
		if(((new String(carAgent.direction).equals("east"))  &&  carAgent.y==agent.y))
		{
			if((agent.x > carAgent.x) && (agent.x - carAgent.x)<distanceBetweenAgents)
			{
				if((new String(agent.direction).equals("null")) && (agent.x==carAgent.x))
					return null;
				distanceBetweenAgents= (agent.x - carAgent.x);
				return agent;
			}
		}
		if(((new String(carAgent.direction).equals("west")) &&  carAgent.y==agent.y))
		{
			if((agent.x <carAgent.x) && (carAgent.x - agent.x)<distanceBetweenAgents)
			{
				if((new String(agent.direction).equals("null")) && (agent.x==carAgent.x))
					return null;
				distanceBetweenAgents= (carAgent.x - agent.x);
				return agent;
			}
		}
		return null;
		
	}
	
	// Zapis danych do plików
	public void SaveAgentsState()
	{
		writeTime=(double)((System.currentTimeMillis()-startTime))/1000;
		for(AgentClass agent:agents)
		{
			try
			{
				PrintWriter outputStream=new PrintWriter(new FileOutputStream((agent.name +".txt"), true));
				//System.out.println("File names:" +agent.name);
				if(outputStream!=null)
				{			
					outputStream.println(writeTime +" " + agent.type +" "+agent.x +" "+agent.y +" "+agent.direction +" "+agent.lightColor);
				}
				outputStream.close();
			}
			catch(FileNotFoundException ex)
			{}
		}
	}
}
		