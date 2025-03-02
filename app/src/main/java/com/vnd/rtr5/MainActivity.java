package com.vnd.rtr5;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ExpandableListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private AssignmentAdapter adapter;
    private List<Assignment> assignmentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            // Show app icon in default action bar in vertical mode
            getSupportActionBar().setIcon(R.mipmap.ic_launcher);
            getSupportActionBar().setDisplayShowHomeEnabled(true);  // To show the icon
            // getSupportActionBar().setDisplayHomeAsUpEnabled(true);  // Optional, if you want a back button
        }

        Button btnHome = findViewById(R.id.btnHome);
        Button btnAbout = findViewById(R.id.btnAbout);

        btnHome.setOnClickListener(view -> {
        });

        btnAbout.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
        });

        ExpandableListView expandableListView = findViewById(R.id.expandableListView);
        assignmentList = createAssignments();
        adapter = new AssignmentAdapter(this, assignmentList);
        expandableListView.setAdapter(adapter);

        // Handle click on child
        expandableListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            Assignment assignment = (Assignment) adapter.getChild(groupPosition, childPosition);
            return startOpenGLActivity(assignment);
        });

        // Handle click on parent
        expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            Assignment assignment = assignmentList.get(groupPosition);
            return startOpenGLActivity(assignment);
        });
    }

    private boolean startOpenGLActivity(Assignment assignment) {
        boolean expansionPossible = false;
        if (assignment.getChildren().isEmpty()) {
            Intent intent = new Intent(MainActivity.this, GLESActivity.class);
            intent.putExtra("className", assignment.getClassName());
            startActivity(intent);
            expansionPossible = true;
        }
        return expansionPossible;
    }

    private List<Assignment> createAssignments() {
        List<Assignment> assignments = new ArrayList<>();

        assignments.add(new Assignment("1. Blue Screen", "x1_blue_screen"));

        assignments.add(new Assignment("2. Orthographic Triangle", "x2_orthographic"));

        Assignment perspective = new Assignment("3. Perspective Triangle", null);
        perspective.addChild(new Assignment("1. Black and White", "x3_perspective.black_and_white"));
        perspective.addChild(new Assignment("2. Color", "x3_perspective.color"));
        assignments.add(perspective);

        Assignment rectangle = new Assignment("4. Rectangle", null);
        rectangle.addChild(new Assignment("1. Black and White", "x4_rectangle.black_and_white"));
        rectangle.addChild(new Assignment("2. Color", "x4_rectangle.color"));
        assignments.add(rectangle);

        Assignment two_2D_shapes = new Assignment("5. Two 2D shapes", null);
        two_2D_shapes.addChild(new Assignment("1. Black and White", "x5_two_2D_shapes.black_and_white"));
        two_2D_shapes.addChild(new Assignment("2. Color", "x5_two_2D_shapes.color"));
        assignments.add(two_2D_shapes);

        Assignment rotation2D = new Assignment("6. 2D Rotation", null);
        rotation2D.addChild(new Assignment("1. Black and White Triangle", "x6_2D_rotation.black_and_white.x1_triangle"));
        rotation2D.addChild(new Assignment("2. Black and White Rectangle", "x6_2D_rotation.black_and_white.x2_rectangle"));
        rotation2D.addChild(new Assignment("3. Two Black and White 2D Shapes", "x6_2D_rotation.black_and_white.x3_two_2D_shapes"));
        rotation2D.addChild(new Assignment("4. Color Triangle", "x6_2D_rotation.color.x1_triangle"));
        rotation2D.addChild(new Assignment("5. Color Rectangle", "x6_2D_rotation.color.x2_rectangle"));
        rotation2D.addChild(new Assignment("6. Two Color 2D Shapes", "x6_2D_rotation.color.x3_two_2D_shapes"));
        assignments.add(rotation2D);

        Assignment rotation3D = new Assignment("7. 3D Rotation", null);
        rotation3D.addChild(new Assignment("1. Black and White Pyramid", "x7_3D_rotation.black_and_white.x1_pyramid"));
        rotation3D.addChild(new Assignment("2. Black and White Cube", "x7_3D_rotation.black_and_white.x2_cube"));
        rotation3D.addChild(new Assignment("3. Two Black and White 3D Shapes", "x7_3D_rotation.black_and_white.x3_two_3D_shapes"));
        rotation3D.addChild(new Assignment("4. Color Pyramid", "x7_3D_rotation.color.x1_pyramid"));
        rotation3D.addChild(new Assignment("5. Color Cube", "x7_3D_rotation.color.x2_cube"));
        rotation3D.addChild(new Assignment("6. Two Color 3D Shapes", "x7_3D_rotation.color.x3_two_3D_shapes"));
        assignments.add(rotation3D);

        Assignment texture = new Assignment("8. Texture", null);
        texture.addChild(new Assignment("1. Pyramid", "x8_texture.x1_pyramid"));
        texture.addChild(new Assignment("2. Cube", "x8_texture.x2_cube"));
        texture.addChild(new Assignment("3. Two 3D Shapes", "x8_texture.x3_two_3D_shapes"));
        texture.addChild(new Assignment("4. Smiley", "x8_texture.x4_smiley"));
        texture.addChild(new Assignment("5. Tweaked Smiley", "x8_texture.x5_tweaked_smiley"));
        texture.addChild(new Assignment("6. Checkerboard", "x8_texture.x6_checkerboard"));
        assignments.add(texture);

        assignments.add(new Assignment("9. White Sphere", "x9_white_sphere"));

        Assignment lights = new Assignment("10. Lights", null);
        lights.addChild(new Assignment("1. Diffused Light On Pyramid", "x10_lights.x1_diffused.x1_pyramid"));
        lights.addChild(new Assignment("2. Diffused Light On Cube", "x10_lights.x1_diffused.x2_cube"));
        lights.addChild(new Assignment("3. Diffused Light On Sphere", "x10_lights.x1_diffused.x3_sphere"));
        lights.addChild(new Assignment("4. Per Vertex Light On White Sphere", "x10_lights.x2_per_vertex.x1_white_sphere"));
        lights.addChild(new Assignment("5. Per Fragment Light On White Sphere", "x10_lights.x3_per_fragment.x1_white_sphere"));
        lights.addChild(new Assignment("6. Per Vertex Light On Albedo Sphere", "x10_lights.x2_per_vertex.x2_albedo_sphere"));
        lights.addChild(new Assignment("7. Per Fragment Light On Albedo Sphere", "x10_lights.x3_per_fragment.x2_albedo_sphere"));
        lights.addChild(new Assignment("8. Two Per Vertex Lights On Pyramid", "x10_lights.x4_two_lights.x1_per_vertex"));
        lights.addChild(new Assignment("9. Two Per Fragment Lights On Pyramid", "x10_lights.x4_two_lights.x2_per_fragment"));
        lights.addChild(new Assignment("10. Toggle Per Vertex, Per Fragment Light", "x10_lights.x5_toggle"));
        lights.addChild(new Assignment("11. Three Moving Lights On Sphere", "x10_lights.x6_three_moving_lights"));
        lights.addChild(new Assignment("12. Per Vertex Light On 24 spheres", "x10_lights.x7_24spheres.x1_per_vertex"));
        lights.addChild(new Assignment("13. Per Fragment Light On 24 spheres", "x10_lights.x7_24spheres.x2_per_fragment"));
        assignments.add(lights);

        assignments.add(new Assignment("11. Graph Paper With Shapes", "x11_graph_paper_with_shapes"));

        Assignment pushPopMatrix = new Assignment("12. Push-Pop Matrix", null);
        pushPopMatrix.addChild(new Assignment("1. Solar System", "x12_push_pop_matrix.x1_solar_system"));
        pushPopMatrix.addChild(new Assignment("2. Robotic arm", "x12_push_pop_matrix.x2_robotic_arm"));
        assignments.add(pushPopMatrix);

        assignments.add(new Assignment("13. Render To Texture", "x13_render_to_texture"));

        Assignment tesselation = new Assignment("14. Tesselation", null);
        tesselation.addChild(new Assignment("1. Line", "x14_tesselation.x1_line"));
        tesselation.addChild(new Assignment("2. Triangle", "x14_tesselation.x2_triangle"));
        tesselation.addChild(new Assignment("3. Quad", "x14_tesselation.x3_quad"));
        assignments.add(tesselation);

        assignments.add(new Assignment("15. Geometry", "x15_geometry"));

        assignments.add(new Assignment("16. Interleave", "x16_interleave"));

        assignments.add(new Assignment("17. Indexed Drawing", "x17_indexed_drawing"));

        return assignments;
    }
}
