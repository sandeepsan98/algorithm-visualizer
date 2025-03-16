

# Algorithm Visualizer

![Algorithm Visualizer Demo](https://via.placeholder.com/800x400.png?text=Algorithm+Visualizer+Demo)  
*Replace the above with an actual screenshot or GIF of your application in action.*

**Algorithm Visualizer** is an interactive web application designed to visualize and understand sorting algorithms. It provides a graphical representation of how algorithms like Quick Sort, Merge Sort, and others manipulate data step-by-step, making it an excellent tool for students, educators, and developers to learn and teach algorithmic concepts. The application supports both JavaScript and Java implementations, allows custom input, and includes features like sound effects, animation speed control, and step-by-step navigation.

---

## Features

- **Supported Algorithms**: Visualize Quick Sort, Insertion Sort, Merge Sort, Heap Sort, Bubble Sort, and Selection Sort.
- **Language Support**: View and execute algorithms in JavaScript or Java.
- **Interactive Visualization**: Bar chart animation with color-coded swaps and comparisons, hover effects, and sound feedback.
- **Code Editor**: Syntax-highlighted code preview with real-time step highlighting using CodeMirror.
- **Custom Input**: Enter your own data (numbers or strings) for sorting, with validation (1-50 elements, numbers 1-100).
- **Controls**: Play/pause animation, step forward/backward, reset, and adjust animation speed.
- **Metrics**: Displays execution time, number of comparisons, and swaps.
- **Responsive Design**: Adapts to various screen sizes with a clean, modern UI.

---

## Technologies Used

### Frontend
- **HTML5**: Structure of the web application.
- **CSS3**: Styling with a responsive design using Flexbox and media queries.
- **JavaScript**: Core logic and interactivity.
- **p5.js**: Visualization of sorting steps with animated bar charts.
- **CodeMirror**: Embedded code editor with syntax highlighting for JavaScript and Java.
- **Libraries**:
  - `p5.js v1.4.2`
  - `CodeMirror v5.65.15` (with `javascript` and `clike` modes, `monokai` theme)

### Backend
- **Java**: Server-side execution of Java code and servlet handling.
- **Servlets**: Handles HTTP requests for code execution via `ExecuteServlet`.
- **Node.js**: Executes JavaScript code on the server side.
- **Gson**: JSON parsing and serialization in Java.

### Tools & Dependencies
- **Maven**: Dependency management for Java (assumed based on typical servlet projects).
- **SLF4J**: Logging framework for Java backend.
- **External APIs**: None; all processing is local or server-side.

---

## APIs Used

### Internal API
- **Endpoint**: `/codeflowpro/execute`
  - **Method**: `POST`
  - **Request Body**:
    ```json
    {
      "code": "string", // The algorithm code to execute
      "language": "string" // "JavaScript" or "Java"
    }
    ```
  - **Response**:
    ```json
    {
      "output": "string", // Final sorted array or error message
      "steps": [ // Array of steps for visualization
        {
          "array": ["value1", "value2", ...], // State of the array
          "line": number // Corresponding code line
        },
        ...
      ],
      "algorithm": "string", // Detected algorithm name
      "executionTime": number, // Execution time in ms
      "isNumeric": boolean, // Whether input was numeric
      "status": "string" // "success" or error status
    }
    ```
  - **Purpose**: Executes the provided code, instruments it for step logging, and returns the output and visualization steps.

No external APIs are used; all functionality is self-contained within the application.

---

## Prerequisites

- **Node.js**: v14.x or higher (for JavaScript execution).
- **Java**: JDK 17 or higher (for Java execution and servlet).
- **Web Browser**: Modern browser (Chrome, Firefox, Edge) with JavaScript enabled.
- **Servlet Container**: Apache Tomcat 9.x or similar (for deploying the Java backend).

---

## Setup Instructions

### Clone the Repository
```bash
git clone https://github.com/yourusername/algorithm-visualizer.git
cd algorithm-visualizer
```

### Frontend Setup
1. **Place Files**: Ensure the HTML (`index.html`) and JavaScript (`script.js`) files are in the `src/main/webapp` directory (or equivalent for your web server).
2. **Dependencies**: The required libraries (p5.js, CodeMirror) are loaded via CDN in the HTML. No additional installation is needed for the frontend.

### Backend Setup
1. **Java Servlet**:
   - Place `ExecuteServlet.java` in `src/main/java/com/project/codeflowpro/servelts/`.
   - Ensure `gson` and `slf4j` dependencies are in your `pom.xml`:
     ```xml
     <dependencies>
         <dependency>
             <groupId>com.google.code.gson</groupId>
             <artifactId>gson</artifactId>
             <version>2.10.1</version>
         </dependency>
         <dependency>
             <groupId>org.slf4j</groupId>
             <artifactId>slf4j-api</artifactId>
             <version>2.0.13</version>
         </dependency>
         <dependency>
             <groupId>org.slf4j</groupId>
             <artifactId>slf4j-simple</artifactId>
             <version>2.0.13</version>
         </dependency>
     </dependencies>
     ```
   - Compile and deploy to a servlet container (e.g., Tomcat).

2. **JavaScript Execution**:
   - Place `execute.js` in `src/main/webapp/js/`.
   - Install Node.js if not already installed:
     ```bash
     npm install -g node
     ```

3. **Build and Deploy**:
   - Use Maven to build the project:
     ```bash
     mvn clean package
     ```
   - Deploy the WAR file to your servlet container.

### Running the Application
- Start your servlet container (e.g., `tomcat/bin/startup.sh`).
- Access the app at `http://localhost:8080/your-app-name/` (adjust port and context path as needed).

---

## Usage Guide

1. **Select Algorithm**: Choose an algorithm from the dropdown (e.g., Quick Sort).
2. **Select Language**: Pick JavaScript or Java to view/edit the code.
3. **Enter Custom Input**: Input comma-separated values (e.g., `5, 2, 9, 1`) in the text field. Leave blank for default input (`64, 34, 25, 12, 22, 11, 90`).
   - **Constraints**: 1-50 elements; numbers must be 1-100; strings are sorted lexicographically.
4. **Visualize**: Click the "Visualize" button to execute and animate the sorting process.
5. **Interact**:
   - **Play/Pause**: Toggle animation with the "Pause" button.
   - **Step Navigation**: Use "Step Forward" and "Step Backward" to move through steps.
   - **Reset**: Restart the visualization.
   - **Speed**: Adjust animation speed with the slider (100-2000ms).
   - **Sound**: Toggle sound effects with the checkbox.
6. **View Output**: Check the sorted result and execution time below the visualizer.

---

## Application Flow

1. **User Input**:
   - User selects algorithm, language, and optionally enters custom input.
   - Input is validated (1-50 elements, numeric range 1-100, or valid strings).

2. **Code Generation**:
   - The selected algorithm’s template is populated with the input.
   - Code is displayed in the CodeMirror editor.

3. **Execution**:
   - On clicking "Visualize," the code is sent to the `/execute` endpoint.
   - **JavaScript**: Node.js executes the code with instrumentation via `execute.js`.
   - **Java**: Servlet compiles and runs the code, adding logging for steps.

4. **Step Logging**:
   - Code is instrumented to log line execution (`LINE:`) and array states (`STATE:`).
   - Steps are collected and returned as JSON.

5. **Visualization**:
   - p5.js renders the array as bars, animating transitions between steps.
   - Colors indicate swaps (red) and sorted states (green).
   - Sound plays on swaps if enabled.
   - Code editor highlights the current line.

6. **Output Display**:
   - Final sorted array and execution time are shown.
   - Metrics (comparisons, swaps) update in real-time during animation.

---

## Project Structure

```
algorithm-visualizer/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/project/codeflowpro/servelts/
│   │   │       └── ExecuteServlet.java
│   │   ├── webapp/
│   │   │   ├── js/
│   │   │   │   ├── script.js
│   │   │   │   └── execute.js
│   │   │   └── index.html
│   └── pom.xml
├── README.md
└── .gitignore
```

---



Please ensure your code follows the existing style and includes tests where applicable.

---

## Known Issues

- **Timeout**: Complex inputs may exceed the 20-second execution timeout.
- **Sound**: Audio may not work in some browsers until user interaction (browser policy).
- **Input Validation**: Mixed numeric/string inputs may cause unexpected behavior.

---

## Future Enhancements

- Add more algorithms (e.g., Radix Sort, Shell Sort).
- Support additional languages (e.g., Python, C++).
- Implement real-time collaboration features.
- Enhance visualization with more detailed step explanations.

---



---

## Acknowledgments

- Inspired by various algorithm visualization tools like VisuAlgo and Sorting.at.
- Thanks to the open-source communities behind p5.js, CodeMirror, and Gson.


