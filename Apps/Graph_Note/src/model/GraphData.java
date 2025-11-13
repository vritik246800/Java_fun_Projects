package model;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO that represents the serializable graph (nodes + edges).
 */
public class GraphData {
    public List<Node> nodes = new ArrayList<>();
    public List<Edge> edges = new ArrayList<>();
}
