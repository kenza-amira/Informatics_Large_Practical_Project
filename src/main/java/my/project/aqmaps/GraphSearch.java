package my.project.aqmaps;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

public class GraphSearch {
	public ArrayList<Integer> greedySearch(double[][] dists, int length, ArrayList<Point> sensorsLocation, Point start) {
		var visited = new ArrayList<Integer>();
		var route = new ArrayList<Integer>();
		var distance = new ArrayList<Double>();
		var helper = new Helpers();
		int counter = 1;
		
		Queue<Integer> queue = new LinkedList<Integer>();
		for (Point p: sensorsLocation) {
			var d = helper.euclid(p.longitude(), p.latitude(), start.longitude(), start.latitude());
			distance.add(d);
		}
		int minIndex = distance.indexOf(Collections.min(distance));
		
		route.add(minIndex);
		queue.add(minIndex);
		
		while (counter < length) {
			counter += 1;
			Integer i = queue.remove();
			visited.add(i);
			double[] next = dists[i];
			for (Integer x : visited) {
				next[x] = 0.0;
			}
			double value = Double.MAX_VALUE;
			for (Double d : next) {
				value = (d == 0) ? value : Math.min(value, d);
			}
			int index = helper.findIndex(next, value);
			queue.add(index);
			visited.add(index);
			route.add(index);
		}
		return route;
	}

	public ArrayList<Point> findNext(Point first) {
		var points = new ArrayList<Point>();
		var copy = first;
		var lat = copy.latitude();
		var lng = copy.longitude();
		for (int angle = 0; angle < 350; angle += 10) {
			var lngAdd = 0.0003 * Math.cos((Math.toRadians(angle)));
			var latAdd = 0.0003 * Math.sin((Math.toRadians(angle)));
			points.add(Point.fromLngLat(lng + lngAdd, lat + latAdd));
		}
		return points;
	}

	public boolean isIntersecting(LineString l, ArrayList<LineString> ls) {
		Point start1 = l.coordinates().get(0);
		Point end1 = l.coordinates().get(1);
		var x1 = start1.longitude();
		var x2 = end1.longitude();
		var y1 = start1.latitude();
		var y2 = end1.latitude();
		for (LineString s : ls) {
			for (int i = 0; i < s.coordinates().size() - 1; i++) {
				Point start2 = s.coordinates().get(i);
				Point end2 = s.coordinates().get(i + 1);
				var x3 = start2.longitude();
				var x4 = end2.longitude();
				var y3 = start2.latitude();
				var y4 = end2.latitude();
				if (Line2D.linesIntersect(x1, y1, x2, y2, x3, y3, x4, y4)) {
					return true;
				}
			}
		}
		return false;
	}

	public ArrayList<LineString> findPath(ArrayList<Point> orderedSensors, ArrayList<String> orderedBatteries,
			ArrayList<String> orderedReadings, Point start, ArrayList<LineString> allZones, ArrayList<Feature> features,
			ArrayList<String> path, ArrayList<String> w3wOrdered) {
		
		// Path Finding algorithm
		int sum = 0;
		var first = Point.fromLngLat(start.longitude(), start.latitude());
		var lines = new ArrayList<LineString>();
		var helper = new Helpers();
		var sensHelp = new SensorHelpers();

		// Added starting point to the end to form closed loop
		orderedSensors.add(start);
		
		while (sum < 150 && !orderedSensors.isEmpty()) {
			var st = String.valueOf(sum + 1) + ",";
			var distance = new ArrayList<Double>();
			var points = new ArrayList<Point>();
			var nextPoints = findNext(first);
			var possible = new ArrayList<LineString>();
			var target = orderedSensors.get(0);
			for (Point p : nextPoints) {
				var temporaryPoints = new ArrayList<Point>();
				var x1 = (Double) p.latitude();
				var x2 = (Double) target.latitude();
				var y1 = (Double) p.longitude();
				var y2 = (Double) target.longitude();
				distance.add(helper.euclid(x1, y1, x2, y2));
				temporaryPoints.add(first);
				temporaryPoints.add(p);
				possible.add(LineString.fromLngLats(temporaryPoints));
		}

/*			for (int k = 0; k< possible.size(); k++) {
				if (isIntersecting(possible.get(k), allZones)) {
					distance.set(k, Double.MAX_VALUE);
					System.out.print(k+",");
				}
			}*/

			int minIndex = distance.indexOf(Collections.min(distance));
			System.out.println(":" +minIndex);
			var nextP = nextPoints.get(minIndex);
			points.add(first);
			points.add(nextP);
			st = st + String.valueOf(first.longitude()) + "," + String.valueOf(first.latitude()) + ","
					+ String.valueOf(minIndex * 10) + "," + String.valueOf(nextP.longitude()) + ","
					+ String.valueOf(nextP.latitude()) + ",";
			
			lines.add(LineString.fromLngLats(points));
			first = nextP;
			
			var feat = Feature.fromGeometry((Geometry) target);
			if (Collections.min(distance) < 0.0002 && orderedSensors.get(0) == start) {
				st += "null";
				path.add(st);
				System.out.println("Moves: "+ sum);
				return lines;
			} else if (Collections.min(distance) < 0.0002) {
				var coloredFeature = sensHelp.Color(orderedReadings.get(0), feat, 
						orderedBatteries.get(0),w3wOrdered.get(0));
				st = st + String.valueOf(w3wOrdered.get(0));
				orderedSensors.remove(0);
				orderedReadings.remove(0);
				orderedBatteries.remove(0);
				w3wOrdered.remove(0);
				features.add(coloredFeature);
			} else {
				st = st + "null";
			}
			
			sum += 1;
			path.add(st);
		}
		System.out.println("150 reached");
		return lines;
	}
}
