import processing.core.PApplet;
import processing.core.PShape;
import processing.core.PVector;
import queasycam.QueasyCam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainApp extends PApplet {

    public interface densityFunction
    {
        float densityFunction(PVector v);
    }

    PShape getTriangulation(PVector cube, float size, int subDivisionCount,densityFunction f,float isoValue )
    {
        //TODO CHANGE EDGES TO USE SOME HASH INSTEAD OF STRING CONCAT
        float step = size/subDivisionCount;
        HashMap<PVector, Float> densityValues = new HashMap<>(subDivisionCount*subDivisionCount*subDivisionCount);
        HashMap<String, PVector> edges = new HashMap<>();
        PShape triangles = createShape(GROUP);
        for (float x = cube.x; x < cube.x +size;x+=step)
        {
            for (float y = cube.y; y < cube.y+size;y+=step)
            {
                for (float z = cube.z; z < cube.z+size;z+=step)
                {
                    PVector position = new PVector(x, y, z);
                    densityValues.putIfAbsent(position, f.densityFunction(position));
                    triangles.addChild(getTriangles(position, step, edges, f, densityValues, isoValue));
                }
            }
        }
        return triangles;
    }


    PShape getTriangles(PVector cubePosition, float cubeSize,HashMap<String, PVector> edges,densityFunction f, HashMap<PVector, Float> densityValues, float isoValue)
    {

        ArrayList<PVector> vertices = new ArrayList(8);
        vertices.add(cubePosition                                                                                 );//0
        vertices.add(new PVector(cubePosition.x + cubeSize, cubePosition.y, cubePosition.z            ));//1
        vertices.add(new PVector(cubePosition.x + cubeSize, cubePosition.y, cubePosition.z + cubeSize));//2
        vertices.add(new PVector(cubePosition.x, cubePosition.y, cubePosition.z + cubeSize));//3
        vertices.add(new PVector(cubePosition.x, cubePosition.y + cubeSize, cubePosition.z            ));//4
        vertices.add(new PVector(cubePosition.x + cubeSize, cubePosition.y + cubeSize, cubePosition.z            ));//5
        vertices.add(new PVector(cubePosition.x + cubeSize, cubePosition.y + cubeSize, cubePosition.z + cubeSize));//6
        vertices.add(new PVector(cubePosition.x, cubePosition.y + cubeSize, cubePosition.z + cubeSize));//7


        int config = 0;

        for (int i = 7; i>=0; i--)
        {
            config = config <<1;
            densityValues.putIfAbsent(vertices.get(i), f.densityFunction(vertices.get(i)));
            if (densityValues.get(vertices.get(i)) >= isoValue)
            {
                config += 1;
            }
        }
        if((config != 0) && (config != 255) )
        {
            drawVert(config, vertices);
            drawCube(vertices);
        }
        int edgeConfig = Table.EDGES[config];
        for(int i= 0 ; i<12; ++i)
        {
            if((edgeConfig >> i & 1) != 0)
            {
                int[] edgeVertices = Table.EDGEVERTEX[i];
                PVector v0 = vertices.get(edgeVertices[0]);
                PVector v1 = vertices.get(edgeVertices[1]);
                edges.putIfAbsent(v0.toString()+v1.toString(),
                        interpolate(
                                vertices.get(edgeVertices[0]),
                                vertices.get(edgeVertices[1]),
                                densityValues.get(vertices.get(edgeVertices[0])),
                                densityValues.get(vertices.get(edgeVertices[1])),
                                isoValue));
            }
        }
        PShape triangles = createShape();
        int[] tab = Table.TRIANGLES[config];
        triangles.beginShape(TRIANGLES);
        triangles.fill(255);
        triangles.stroke(0,255,255);
        triangles.noStroke();
        triangles.strokeWeight(1);
        for(int i = 0; i<15; i+=3)
                {
                    if (tab[i] == -1)
                        break;
                    for(int j =0; j<3;j++) {
                        int[] v = Table.EDGEVERTEX[tab[i+j]];
                        PVector v0 = vertices.get(v[0]);
                        PVector v1 = vertices.get(v[1]);
                        PVector edge = edges.get(v0.toString()+v1.toString());
                        triangles.vertex(edge.x, edge.y, edge.z);
            }
        }
        triangles.endShape();
        return triangles;
    }

    PVector interpolate(PVector p1, PVector p2, float v1, float v2, float isovalue)
    {
        if (abs(isovalue-v1) < 0.00001)
        return(p1);
        if (abs(isovalue-v2) < 0.00001)
            return(p2);
        if (abs(v1-v2) < 0.00001)
            return(p1);

        PVector pv =  p1.copy().add(p2.copy().sub(p1).mult((isovalue-v1)/(v2-v1)));
        return pv;
    }

    void drawVert(int value, ArrayList<PVector> vertices)
    {
        strokeWeight(8);
        stroke(0, 255, 0);
        fill(0, 255, 0);
        beginShape(POINTS);
        for (int i=0; i<8; ++i)
        {
            if ((value & 1) != 0 )
            {
                vertex(vertices.get(i).x, vertices.get(i).y, vertices.get(i).z);
            }
            value= value >> 1;
        }
        endShape();
    }

    void drawCube(ArrayList<PVector> vertices)
    {
        noFill();
        stroke(255,0,0);
        strokeWeight(1);
        beginShape(LINES);
        for (int i =0;i<12;++i)
        {
            int[] v = Table.EDGEVERTEX[i];
            PVector pv0 = vertices.get(v[0]);
            PVector pv1 = vertices.get(v[1]);

            vertex(pv0.x, pv0.y, pv0.z);
            vertex(pv1.x, pv1.y, pv1.z);
        }
        endShape();
    }

    public float sphereDistanceFunction(PVector p,PVector spherePos, float radius)
    {
        return max(radius - p.dist(spherePos),0)/radius;
    }

    public static void main(String[] args) {
        PApplet.main("MainApp", args);
    }
    QueasyCam cam;
    PShape triangulation;

    public void setup()
    {
        noiseSeed(4);
        cam = new QueasyCam(this);
        cam.controllable = true;
        cam.sensitivity = 1;
        triangulation = getTriangulation(
                new PVector(-50,-50,-50),
                100,
                50,
                (p) -> sphereDistanceFunction(p, new PVector(0,0,0),50) + noise(p.x*0.03f,p.y*0.03f,p.z*0.03f)*0.5f
                ,0.5f);

    }

    public void draw()
    {

        lights();
        println(frameRate);
        background(0);
        shape(triangulation);

    }
   public void settings()
   {
       size(800, 800, P3D);
       smooth(4);
   }

}
