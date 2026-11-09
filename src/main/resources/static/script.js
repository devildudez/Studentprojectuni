
// =====================================
// STUDENT SPACE — JAVASCRIPT
// =====================================

const $ = (id) => document.getElementById(id);

const API = "/api/v1";

let token = sessionStorage.getItem("studentToken") || "";
let roles = [];
let students = [];
let editingId = null;
let toastTimer;

try {
    roles = JSON.parse(sessionStorage.getItem("studentRoles") || "[]");
} catch {
    roles = [];
}


// =====================================
// 1. CONNECT TO BACKEND
// =====================================

async function apiRequest(path, options = {}, authorized = true) {
    const headers = {
        "Content-Type": "application/json"
    };

    if (authorized && token) {
        headers.Authorization = `Bearer ${token}`;
    }

    let response;

    try {
        response = await fetch(API + path, {
            ...options,
            headers
        });
    } catch {
        throw new Error("Cannot connect to the server.");
    }

    const text = await response.text();

    let data = null;

    if (text) {
        try {
            data = JSON.parse(text);
        } catch {
            data = null;
        }
    }

    if (!response.ok) {
        if (response.status === 401 && authorized) {
            logout();
        }

        if (response.status === 401) {
            throw new Error("Invalid login or expired session.");
        }

        if (response.status === 403) {
            throw new Error("You do not have permission.");
        }

        throw new Error(
            data?.message ||
            data?.error ||
            `Server error: ${response.status}`
        );
    }

    return data;
}


// =====================================
// 2. LOGIN
// =====================================

$("loginForm").addEventListener("submit", async (event) => {
    event.preventDefault();

    const email = $("loginEmail").value.trim();
    const password = $("loginPassword").value;

    $("loginError").hidden = true;
    $("loginButton").disabled = true;
    $("loginButton").textContent = "Signing in...";

    try {
        const data = await apiRequest("/auth/login", {
            method: "POST",
            body: JSON.stringify({ email, password })
        }, false);

        token = data.token;
        roles = data.roles || [];

        sessionStorage.setItem("studentToken", token);
        sessionStorage.setItem("studentRoles", JSON.stringify(roles));

        $("loginPassword").value = "";

        showDashboard();
        await loadStudents();

    } catch (error) {
        $("loginError").textContent = error.message;
        $("loginError").hidden = false;
    } finally {
        $("loginButton").disabled = false;
        $("loginButton").textContent = "Sign in →";
    }
});


// =====================================
// 3. DASHBOARD AND PERMISSIONS
// =====================================

function isAdmin() {
    return roles.includes("ROLE_ADMIN") ||
           roles.includes("ADMIN");
}

function showDashboard() {
    $("loginScreen").hidden = true;
    $("dashboard").hidden = false;

    let roleName = "School team";

    if (isAdmin()) {
        roleName = "Admin";
    } else if (
        roles.includes("ROLE_TEACHER") ||
        roles.includes("TEACHER")
    ) {
        roleName = "Teacher";
    } else if (
        roles.includes("ROLE_SUPER_ADMIN") ||
        roles.includes("SUPER_ADMIN")
    ) {
        roleName = "Super Admin";
    }

    $("userRole").textContent = roleName;
    $("accessText").textContent = roleName;

    // Only admins can add students
    $("addStudentButton").hidden = !isAdmin();
    $("heroAddButton").hidden = !isAdmin();
}

function logout() {
    token = "";
    roles = [];
    students = [];

    sessionStorage.removeItem("studentToken");
    sessionStorage.removeItem("studentRoles");

    $("dashboard").hidden = true;
    $("loginScreen").hidden = false;
    $("loginForm").reset();
}

$("logoutButton").addEventListener("click", logout);


// =====================================
// 4. LOAD STUDENTS
// =====================================

async function loadStudents() {
    try {
        const data = await apiRequest("/student");

        students = Array.isArray(data.response)
            ? data.response
            : [];

        $("studentCount").textContent =
            data.totalCount ?? students.length;

        renderStudents();

    } catch (error) {
        students = [];

        $("studentCount").textContent = "—";
        $("studentRows").replaceChildren();
        $("emptyState").hidden = true;
        $("listLabel").textContent = "Unable to load students";

        showToast(error.message);
    }
}


// =====================================
// 5. DISPLAY STUDENTS
// =====================================

function renderStudents() {
    const search = $("searchInput").value
        .toLowerCase()
        .trim();

    const filtered = students.filter((student) => {
        const fullName =
            `${student.name || ""} ${student.surname || ""}`
                .toLowerCase();

        return fullName.includes(search) ||
               String(student.id).includes(search);
    });

    const table = $("studentRows");
    table.replaceChildren();

    $("listLabel").textContent =
        `${filtered.length} students shown`;

    $("emptyState").hidden = filtered.length > 0;

    filtered.forEach((student) => {
        const row = document.createElement("tr");

        // Student name and avatar
        const nameCell = row.insertCell();
        const info = document.createElement("div");
        info.className = "student-info";

        const avatar = document.createElement("div");
        avatar.className = "student-avatar";
        avatar.textContent =
            (student.name || "?").charAt(0).toUpperCase();

        const name = document.createElement("span");
        name.className = "student-name";
        name.textContent =
            `${student.name || ""} ${student.surname || ""}`;

        info.append(avatar, name);
        nameCell.append(info);

        // Student ID
        const idCell = row.insertCell();
        idCell.className = "student-id";
        idCell.textContent = `#${student.id}`;

        // Actions
        const actionsCell = row.insertCell();
        const actions = document.createElement("div");
        actions.className = "action-buttons";

        const detailsButton = makeButton("Details", () => {
            openStudent(student.id, "view");
        });

        actions.append(detailsButton);

        if (isAdmin()) {
            const editButton = makeButton("Edit", () => {
                openStudent(student.id, "edit");
            });

            const deleteButton = makeButton("Delete", () => {
                deleteStudent(student.id);
            }, true);

            actions.append(editButton, deleteButton);
        }

        actionsCell.append(actions);
        table.append(row);
    });
}

function makeButton(label, action, destructive = false) {
    const button = document.createElement("button");

    button.type = "button";
    button.textContent = label;

    button.className = destructive
        ? "action-button delete"
        : "action-button";

    button.addEventListener("click", action);

    return button;
}


// =====================================
// 6. SEARCH
// =====================================

$("searchInput").addEventListener("input", renderStudents);


// =====================================
// 7. ADD STUDENT
// =====================================

function addStudent() {
    if (!isAdmin()) return;

    editingId = null;

    $("studentForm").reset();
    $("formError").hidden = true;

    $("dialogTitle").textContent = "Add a student";
    $("saveStudentButton").textContent = "Save student";
    $("saveStudentButton").hidden = false;
    $("editNote").hidden = true;

    ["studentName", "studentSurname",
     "studentPhone", "studentEmail"].forEach((id) => {
        $(id).disabled = false;
    });

    $("studentDialog").showModal();
}

$("addStudentButton").addEventListener("click", addStudent);
$("heroAddButton").addEventListener("click", addStudent);


// =====================================
// 8. VIEW OR EDIT STUDENT
// =====================================

async function openStudent(id, mode) {
    try {
        const student = await apiRequest(`/student/${id}`);

        editingId = id;

        $("studentName").value = student.name || "";
        $("studentSurname").value = student.surname || "";
        $("studentPhone").value = student.phoneNumber || "";
        $("studentEmail").value = student.email || "";

        $("formError").hidden = true;

        const editing = mode === "edit" && isAdmin();

        $("dialogTitle").textContent = editing
            ? "Edit student"
            : "Student details";

        // Backend only permits editing phone and email
        $("studentName").disabled = true;
        $("studentSurname").disabled = true;
        $("studentPhone").disabled = !editing;
        $("studentEmail").disabled = !editing;

        $("editNote").hidden = !editing;
        $("saveStudentButton").hidden = !editing;

        $("saveStudentButton").textContent = "Save changes";

        $("studentDialog").showModal();

    } catch (error) {
        showToast(error.message);
    }
}


// =====================================
// 9. SAVE STUDENT
// =====================================

$("studentForm").addEventListener("submit", async (event) => {
    event.preventDefault();

    if (!isAdmin()) return;

    const name = $("studentName").value.trim();
    const surname = $("studentSurname").value.trim();
    const phoneNumber = $("studentPhone").value.trim();
    const email = $("studentEmail").value.trim();

    const saveButton = $("saveStudentButton");
    saveButton.disabled = true;

    $("formError").hidden = true;

    try {
        if (editingId === null) {
            // CREATE
            await apiRequest("/student/save", {
                method: "POST",
                body: JSON.stringify({
                    name,
                    surname,
                    phone_number: phoneNumber,
                    email
                })
            });

            showToast("Student added successfully! 🌸");

        } else {
            // UPDATE
            await apiRequest(`/student/${editingId}`, {
                method: "PUT",
                body: JSON.stringify({
                    phoneNumber,
                    email
                })
            });

            showToast("Student updated successfully! ✨");
        }

        $("studentDialog").close();

        editingId = null;

        await loadStudents();

    } catch (error) {
        $("formError").textContent = error.message;
        $("formError").hidden = false;
    } finally {
        saveButton.disabled = false;
    }
});


// =====================================
// 10. DELETE STUDENT
// =====================================

async function deleteStudent(id) {
    if (!isAdmin()) return;

    const confirmed = confirm(
        "Are you sure you want to delete this student?"
    );

    if (!confirmed) return;

    try {
        await apiRequest(`/student/${id}`, {
            method: "DELETE"
        });

        showToast("Student deleted successfully.");
        await loadStudents();

    } catch (error) {
        showToast(error.message);
    }
}


// =====================================
// 11. CLOSE STUDENT WINDOW
// =====================================

$("closeDialog").addEventListener("click", () => {
    $("studentDialog").close();
});

$("cancelDialog").addEventListener("click", () => {
    $("studentDialog").close();
});


// =====================================
// 12. NOTIFICATION
// =====================================

function showToast(message) {
    const toast = $("toast");

    toast.textContent = message;
    toast.hidden = false;

    clearTimeout(toastTimer);

    toastTimer = setTimeout(() => {
        toast.hidden = true;
    }, 3500);
}


// =====================================
// 13. RESTORE LOGIN ON REFRESH
// =====================================

if (token && roles.length > 0) {
    showDashboard();
    loadStudents();
}
