package org.processmining.palia.plugins;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.NodeID;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.elements.Activity;
import org.processmining.models.graphbased.directed.bpmn.elements.Event;
import org.processmining.models.graphbased.directed.bpmn.elements.Event.EventType;
import org.processmining.models.graphbased.directed.bpmn.elements.Flow;
import org.processmining.models.graphbased.directed.bpmn.elements.Gateway;
import org.processmining.models.graphbased.directed.bpmn.elements.Gateway.GatewayType;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.DotEdge;
import org.processmining.plugins.graphviz.dot.DotNode;
import org.processmining.plugins.graphviz.visualisation.DotPanel;

public class GraphvizBPMNVisualization {

	@Plugin(
			name = "Graphviz BPMN visualisation",
			returnLabels = { "Dot visualization" },
			returnTypes = { JComponent.class },
			parameterLabels = { "Petri net" },
			userAccessible = true
	)
	@Visualizer
	@UITopiaVariant(
			affiliation = "DTU",
			author = "Andrea Burattin",
			email = "andbur" + (char) 0x40 + "dtu.dk"
	)
	public JComponent visualize(PluginContext context, BPMNDiagram diagram) {
		Dot dot = new Dot();
		dot.setOption("rankdir", "LR");
		dot.setOption("outputorder", "edgesfirst");
		
		Map<NodeID, DotNode> idToNodes = new HashMap<>();
		
		for (Event e : diagram.getEvents()) {
			DotNode n = makeEventNode(dot, e.getEventType() == EventType.END);
			idToNodes.put(e.getId(), n);
		}
		
		for (Activity a : diagram.getActivities()) {
			DotNode n = makeActivityNode(dot, a.getLabel());
			idToNodes.put(a.getId(), n);
		}
		
		for (Gateway g : diagram.getGateways()) {
			String type = "";
			if (g.getGatewayType() == GatewayType.DATABASED) {
				type = "&times;";
			} else if (g.getGatewayType() == GatewayType.PARALLEL) {
				type = "+";
			}
			DotNode n = makeGatewayNode(dot, type);
			idToNodes.put(g.getId(), n);
		}
		
		for (Flow f : diagram.getFlows()) {
			makeEdge(dot, idToNodes.get(f.getSource().getId()), idToNodes.get(f.getTarget().getId()));
		}
		
		DotPanel p = new DotPanel(dot);
		p.setBackground(Color.white);
		return p;
	}
	
	private static DotEdge makeEdge(Dot dot, DotNode source, DotNode target) {
		DotEdge edge = dot.addEdge(source, target);
		edge.setOption("tailclip", "false");
		return edge;
	}

	private static DotNode makeActivityNode(Dot dot, String name) {
		DotNode dotNode = dot.addNode(name);
		dotNode.setOption("shape", "box");
		dotNode.setOption("style", "rounded,filled");
		dotNode.setOption("fillcolor", "#FFFFCC");
		dotNode.setOption("fontname", "Arial");
		dotNode.setOption("fontsize", "8");
		return dotNode;
	}

	private static DotNode makeEventNode(Dot dot, boolean isStart) {
		DotNode dotNode = dot.addNode("");
		dotNode.setOption("shape", "circle");
		dotNode.setOption("style", "filled");
		dotNode.setOption("fillcolor", "white");
		dotNode.setOption("fontcolor", "white");
		dotNode.setOption("width", "0.3");
		dotNode.setOption("height", "0.3");
		dotNode.setOption("fixedsize", "true");
		if (isStart) {
			dotNode.setOption("penwidth", "3");
		}
		return dotNode;
	}

	private static DotNode makeGatewayNode(Dot dot, String name) {
		DotNode dotNode = dot.addNode(
				"<<table border='0'><tr><td></td></tr><tr><td valign='bottom'>" + name + "</td></tr></table>>");
		dotNode.setOption("shape", "diamond");
		dotNode.setOption("style", "filled");
		dotNode.setOption("fillcolor", "white");
		dotNode.setOption("fontcolor", "black");
		dotNode.setOption("fontname", "Arial");

		dotNode.setOption("width", "0.4");
		dotNode.setOption("height", "0.4");
		dotNode.setOption("fontsize", "30");
		dotNode.setOption("fixedsize", "true");

		return dotNode;
	}
}
