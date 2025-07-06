# R* Tree Implementation in Java

## 📌 Project Overview

This project implements a **secondary-memory data structure** designed to efficiently organize **multidimensional data**.

The core structure is an **R\*** Tree — an improved version of the classic R-Tree — implemented in **Java**.

### ✅ Supported Operations

- 📥 Record **insertion**
- 🗑️ Record **deletion**
- 📦 **Range queries**
- 📍 **k-Nearest Neighbors (kNN)** queries
- 🌄 **Skyline queries**
- 🧱 **Bottom-up bulk loading** using the **Sort-Tile-Recursive (STR)** technique

---

## 📊 Performance Experiments

The implementation includes examples and performance comparisons for:

- ⏱️ **Construction time**:
  - Element-wise insertion vs. bulk loading
- 🧭 **Range queries**:
  - R\* Tree vs. sequential search
- 🗺️ **Area size scaling**:
  - Varying the range R in range queries
- 🔍 **kNN scaling**:
  - Increasing the number of nearest neighbors (k)

---

## 🌍 Dataset

The R\* Tree is built using **real spatial data** (latitude and longitude) obtained from **OpenStreetMap**.

Each record represents a **geographical location** in 2D:
- `latitude`
- `longitude`

---

## 🌲 About the R\* Tree

The **R\*** Tree is an **indexing structure** used for efficient storage and retrieval of **multidimensional spatial data**.  
It extends the classic R-Tree with techniques aimed at reducing **overlap** and **coverage** between internal nodes.

### 🔧 Key Improvements

- **Re-insertion**:  
  When a node overflows, it **re-inserts** entries instead of splitting immediately, improving spatial placement.

- **Better split criteria**:  
  Splits consider **overlap** and **distribution** — not just area.

- **Improved insertion path selection**:  
  Chooses child nodes based on **MBR expansion cost** and **overlap minimization**.

---

## 🛠 Technologies Used

- Language: **Java**
- Visualization: **Java 2D Graphics**
- Dataset: **OpenStreetMap-based real coordinates**

---

## 📫 Contact

Feel free to reach out if you have questions or suggestions!

---

