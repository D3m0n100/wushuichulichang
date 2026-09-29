<template>
  <div class="model-viewer">
    <canvas id="renderCanvas"></canvas>
    
    <!-- 控制面板 -->
    <div class="control-panel">
      <!-- 物体1：搅拌机1（固定绕Z轴） -->
      <div class="control-group">
        <span class="object-name">num1_jiaobanji</span>
        <button @click="toggleRotation('part1')" class="control-btn" :class="{ active: isRotating.part1 }">
          {{ isRotating.part1 ? '停止' : '运行' }}
        </button>
      </div>
      
      <!-- 物体2：搅拌机2（固定绕Z轴） -->
      <div class="control-group">
        <span class="object-name">num2_jiaobanji</span>
        <button @click="toggleRotation('part2')" class="control-btn" :class="{ active: isRotating.part2 }">
          {{ isRotating.part2 ? '停止' : '运行' }}
        </button>
      </div>
      
      <!-- 物体3：水泵1（固定绕Y轴） -->
      <div class="control-group">
        <span class="object-name">num1_shuibeng</span>
        <button @click="toggleRotation('part3')" class="control-btn" :class="{ active: isRotating.part3 }">
          {{ isRotating.part3 ? '停止' : '运行' }}
        </button>
      </div>
      
      <!-- 物体4：水泵2（固定绕Y轴） -->
      <div class="control-group">
        <span class="object-name">num2_shuibeng</span>
        <button @click="toggleRotation('part4')" class="control-btn" :class="{ active: isRotating.part4 }">
          {{ isRotating.part4 ? '停止' : '运行' }}
        </button>
      </div>

      <!-- 物体5：水泵3（固定绕Y轴） -->
      <div class="control-group">
        <span class="object-name">num3_shuibeng</span>
        <button @click="toggleRotation('part5')" class="control-btn" :class="{ active: isRotating.part5 }">
          {{ isRotating.part5 ? '停止' : '运行' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import * as BABYLON from 'babylonjs';
import 'babylonjs-loaders';

export default {
  name: 'ModelViewer',
  props: {
    kgValue: {
      type: Number,
      default: 1
    }
  },
  data() {
    return {
      part1: null,
      part2: null,
      part3: null,
      part4: null,
      part5: null,
      isRotating: {
        part1: false,
        part2: false,
        part3: false,
        part4: false,
        part5: false
      },
      // 每个物体的固定旋转轴
      axisMap: {
        part1: 'z',   // 搅拌机1 绕 Z 轴
        part2: 'z',   // 搅拌机2 绕 Z 轴
        part3: 'y',   // 水泵1 绕 Y 轴
        part4: 'y',   // 水泵2 绕 Y 轴
        part5: 'y'    // 水泵3 绕 Y 轴
      },
      speedMap: {
        part1: 0.05,
        part2: 0.05,
        part3: 0.05,
        part4: 0.05,
        part5: 0.05
      },
      scene: null,
      engine: null
    };
  },
  watch: {
    kgValue(newVal) {
      console.log("kgValue 变化:", newVal);
    }
  },
  mounted() {
    console.log("🔥 ModelViewer 组件已挂载, kgValue:", this.kgValue);
    this.$nextTick(() => {
      this.initBabylon();
    });
  },
  beforeUnmount() {
    if (this.engine) {
      this.engine.dispose();
    }
  },
  methods: {
    initBabylon() {
      const canvas = document.getElementById('renderCanvas');
      if (!canvas) {
        console.error("找不到 canvas 元素");
        return;
      }
      
      this.engine = new BABYLON.Engine(canvas, true);
      
      const createScene = () => {
        const scene = new BABYLON.Scene(this.engine);
        scene.clearColor = new BABYLON.Color4(0.05, 0.1, 0.2, 1);
        
        const camera = new BABYLON.ArcRotateCamera(
          "camera", 
          3.2, // 水平角度（绕Y轴旋转）
          0.9, // 垂直角度（俯仰角）
          55, // 相机距离（远近）
           new BABYLON.Vector3(-1, 6, 4),  // 🔥 改成模型中心点
          scene
        );
        //camera.attachControl(canvas, true);
        camera.lowerRadiusLimit = 2;
        camera.upperRadiusLimit = 25;

        const ambientLight = new BABYLON.HemisphericLight("light", BABYLON.Vector3.Up(), scene);
        ambientLight.intensity = 0.7;
        const dirLight = new BABYLON.DirectionalLight("dirLight", new BABYLON.Vector3(-1, -2, 1), scene);
        dirLight.intensity = 0.8;

        BABYLON.SceneLoader.ImportMesh("", "/", "cgs.glb", scene, (meshes) => {
          console.log("模型加载成功", meshes);
          
          this.part1 = scene.getMeshByName("num1_jiaobanji");
          this.part2 = scene.getMeshByName("num2_jiaobanji");
          this.part3 = scene.getMeshByName("num1_shuibeng");
          this.part4 = scene.getMeshByName("num2_shuibeng");
          this.part5 = scene.getMeshByName("num3_shuibeng");
          
          const parts = [this.part1, this.part2, this.part3, this.part4, this.part5];
          const partNames = ["num1_jiaobanji", "num2_jiaobanji", "num1_shuibeng", "num2_shuibeng", "num3_shuibeng"];
          
          parts.forEach((part, index) => {
            if (part) {
              part.rotationQuaternion = null;
              console.log("✅ 找到了物体:", part.name);
            } else {
              console.warn(`❌ 未找到物体: ${partNames[index]}`);
            }
          });
          
          console.log("=== 场景中所有物体 ===");
          scene.meshes.forEach(m => console.log("  -", m.name));
        });
        
        return scene;
      };
      
      this.scene = createScene();
      window.debugScene = this.scene;  // 添加这行，暴露到全局
      this.engine.runRenderLoop(() => {
        if (this.scene) {
          const shouldRotate = this.kgValue === 1;
          
          if (shouldRotate) {
            // 物体1：搅拌机1（绕 Z 轴）
            if (this.isRotating.part1 && this.part1) {
              this.rotateByAxis(this.part1, this.axisMap.part1, this.speedMap.part1);
            }
            // 物体2：搅拌机2（绕 Z 轴）
            if (this.isRotating.part2 && this.part2) {
              this.rotateByAxis(this.part2, this.axisMap.part2, this.speedMap.part2);
            }
            // 物体3：水泵1（绕 Y 轴）
            if (this.isRotating.part3 && this.part3) {
              this.rotateByAxis(this.part3, this.axisMap.part3, this.speedMap.part3);
            }
            // 物体4：水泵2（绕 Y 轴）
            if (this.isRotating.part4 && this.part4) {
              this.rotateByAxis(this.part4, this.axisMap.part4, this.speedMap.part4);
            }
            // 物体5：水泵3（绕 Y 轴）
            if (this.isRotating.part5 && this.part5) {
              this.rotateByAxis(this.part5, this.axisMap.part5, this.speedMap.part5);
            }
          }
          
          this.scene.render();
        }
      });
      
      window.addEventListener('resize', () => {
        if (this.engine) {
          this.engine.resize();
        }
      });
    },
    
    rotateByAxis(mesh, axis, speed) {
      if (!mesh) return;
      mesh.rotationQuaternion = null;
      switch (axis) {
        case 'x': mesh.rotation.x += speed; break;
        case 'y': mesh.rotation.y += speed; break;
        case 'z': mesh.rotation.z += speed; break;
        default: mesh.rotation.y += speed;
      }
    },
    
    toggleRotation(partKey) {
      this.isRotating[partKey] = !this.isRotating[partKey];
      const partNameMap = {
        part1: this.part1?.name || 'num1_jiaobanji',
        part2: this.part2?.name || 'num2_jiaobanji',
        part3: this.part3?.name || 'num1_shuibeng',
        part4: this.part4?.name || 'num2_shuibeng',
        part5: this.part5?.name || 'num3_shuibeng'
      };
      console.log(`${partNameMap[partKey]} ${this.isRotating[partKey] ? '开始旋转' : '停止旋转'}`);
    },
    
    stopAllRotation() {
      this.isRotating = {
        part1: false,
        part2: false,
        part3: false,
        part4: false,
        part5: false
      };
      console.log("所有物体已停止旋转");
    }
  }
}
</script>

<style scoped>
.model-viewer {
  width: 100%;
  height: 100%;
  position: relative;
  min-height: 400px;
}

#renderCanvas {
  width: 100%;
  height: 100%;
  display: block;
  background-color: #0F172A;
  border-radius: 8px;
  touch-action: none;
}

.control-panel {
  position: absolute;
  bottom: 20px;
  left: 20px;
  right: 20px;
  z-index: 100;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: none;
  border-radius: 12px;
  padding: 12px 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: flex-start;
  pointer-events: auto;
  box-sizing: border-box;
}

.control-group {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(30, 40, 50, 0.9);
  padding: 6px 12px;
  border-radius: 8px;
  flex-wrap: nowrap;
}

.object-name {
  color: #7ec8e0;
  font-size: 0.8125rem;
  font-weight: 600;
  white-space: nowrap;
}

.control-btn {
  padding: 6px 16px;
  background-color: #00e8ff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.8125rem;
  font-weight: 500;
  transition: all 0.2s;
  min-width: 52px;
}

.control-btn.active {
  background-color: #f56c6c;
}

.control-btn:hover {
  opacity: 0.85;
  transform: scale(1.02);
}
</style>