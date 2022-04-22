package org.processmining.palia.plugins;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagramFactory;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.bpmn.elements.Event;
import org.processmining.models.graphbased.directed.bpmn.elements.Event.EventType;
import org.processmining.models.graphbased.directed.bpmn.elements.Gateway.GatewayType;

import palia.algorithm.Palia;
import palia.model.Node;
import palia.model.TPA;
import palia.model.Transition;

public class PaliaMiner {
	
	@Plugin(
			name = "Palia Miner",
			parameterLabels = { "Log used for mining" },
			returnLabels = { "Mined BPMN" },
			returnTypes = { BPMNDiagram.class },
			userAccessible = true
		)
	@UITopiaVariant(
		affiliation = "ITACA-SABIEN-PM4H Lab / DTU",
		author = "Carlos Fernandez Llatas / Andrea Burattin",
		email = "cfllatas" + (char) 0x40 + "itaca.upv.es / andbur" + (char) 0x40 + "dtu.dk",
		pack = "PALIA"
	)
	public BPMNDiagram run(PluginContext context, XLog log) {
		context.getProgress().setMinimum(0);
		context.getProgress().setMaximum(9);
		
		Palia p = new Palia();
		TPA tpa = p.mine(log, (status, hasStarted) -> {
			context.getProgress().setCaption(status.toString());
			if (!hasStarted) {
				context.getProgress().setValue(context.getProgress().getValue() + 1);
			}
		});
		return convert(tpa);
	}

	public BPMNDiagram convert(TPA tpa) {
		BPMNDiagram diagram = BPMNDiagramFactory.newBPMNDiagram("");
		
		Event startEvent = diagram.addEvent("", EventType.START, null, null, true, null);
		Event endEvent = diagram.addEvent("", EventType.END, null, null, true, null);
		
		Map<UUID, BPMNNode> idToStartNodes = new HashMap<>();
		Map<UUID, BPMNNode> idToTargetNodes = new HashMap<>();
		
		for (Node n : tpa.getNodes()) {
			BPMNNode activityNode = diagram.addActivity(n.getName(), false, false, false, false, false);
			idToStartNodes.put(n.getId(), activityNode);
			idToTargetNodes.put(n.getId(), activityNode);

			if (n.isStartingNode()) {
				diagram.addFlow(startEvent, activityNode, null);
			}
			if (n.isFinalNode()) {
				diagram.addFlow(activityNode, endEvent, null);
			}
		}
		
		for (Node n : tpa.getNodes()) {
			Set<Transition> outgoing = n.getOutTransitions(false);
			if (outgoing.size() > 1) {
				BPMNNode gateway = diagram.addGateway(null, GatewayType.DATABASED);
				diagram.addFlow(idToStartNodes.get(n.getId()), gateway, null);
				idToStartNodes.put(n.getId(), gateway);
			}

			Set<Transition> incoming = n.getInTransitions(false);
			if (incoming.size() > 1) {
				BPMNNode gateway = diagram.addGateway(null, GatewayType.DATABASED);
				diagram.addFlow(gateway, idToTargetNodes.get(n.getId()), null);
				idToTargetNodes.put(n.getId(), gateway);
			}
		}
		
		for (Transition t : tpa.getTransitions()) {
			Collection<Node> sources = t.getSourceNodes();
			Collection<Node> targets = t.getEndNodes();

			// sequence flow or XOR gateway
			if (sources.size() == 1 && targets.size() == 1) {
				BPMNNode sourceNode = idToStartNodes.get(sources.stream().findAny().get().getId());
				BPMNNode targetNode = idToTargetNodes.get(targets.stream().findAny().get().getId());
				diagram.addFlow(sourceNode, targetNode, null);
			}

			// parallel split
			if (sources.size() == 1 && targets.size() > 1) {
				BPMNNode sourceNode = idToStartNodes.get(sources.stream().findAny().get().getId());
				BPMNNode gateway = diagram.addGateway(null, GatewayType.PARALLEL);
				diagram.addFlow(sourceNode, gateway, null);
				sourceNode = gateway;

				for (Node target : targets) {
					BPMNNode targetNode = idToTargetNodes.get(target.getId());
					diagram.addFlow(sourceNode, targetNode, null);
				}
			}

			// parallel join
			if (sources.size() > 1 && targets.size() == 1) {
				BPMNNode targetNode = idToTargetNodes.get(targets.stream().findAny().get().getId());
				BPMNNode gateway = diagram.addGateway(null, GatewayType.PARALLEL);
				diagram.addFlow(gateway, targetNode, null);
				targetNode = gateway;

				for (Node source : sources) {
					BPMNNode sourceNode = idToStartNodes.get(source.getId());
					diagram.addFlow(sourceNode, targetNode, null);
				}
			}

			// parallel join AND parallel split
			if (sources.size() > 1 && targets.size() > 1) {
				BPMNNode gateway1 = diagram.addGateway(null, GatewayType.PARALLEL);
				BPMNNode gateway2 = diagram.addGateway(null, GatewayType.PARALLEL);
				diagram.addFlow(gateway1, gateway2, null);
				for (Node source : sources) {
					diagram.addFlow(idToStartNodes.get(source.getId()), gateway1, null);
				}
				for (Node target : targets) {
					diagram.addFlow(gateway2, idToTargetNodes.get(target.getId()), null);
				}
			}
		}
		
		return diagram;
	}
}
