# BPMN Auto Layout Bundle

Bundle for [bpmn-auto-layout](https://github.com/bpmn-io/bpmn-auto-layout) library with all dependencies for use with GraalVM.

## Purpose

GraalVM cannot resolve ES module dependencies (like `bpmn-moddle`, `min-dash`) when loading JavaScript modules. 
This bundle packages all dependencies into a single file that GraalVM can execute.

## How to Update Library Version

### 1. Install new version

```bash
npm install bpmn-auto-layout@<version>
```

Example:
```bash
npm install bpmn-auto-layout@1.0.2
```

### 2. Build bundle

```bash
npx rollup -c
```

### 3. Deploy to Spring Boot

```bash
cp dist/bpmn-auto-layout-bundle.umd.js ../src/main/resources/js/bpmn-auto-layout/bundle.umd.js
```

### 4. Test

```bash
cd ..
./mvnw test -Dtest=BpmnAutoLayoutServiceTest
```

## Check Available Versions

```bash
npm view bpmn-auto-layout versions --json
```
