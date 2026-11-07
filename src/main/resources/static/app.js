
const $ = (id) => document.getElementById(id);

let currentUser = null;
let currentType = null;
let currentProfile = null;
let profiles = [];

async function api(url, options = {}) {
    const response = await fetch(url, {
        credentials: "same-origin",
        ...options,
        headers: {
            ...(options.body ? { "Content-Type": "application/json" } : {}),
            ...options.headers
        }
    });

    // Server session is gone (timeout or restart) but the page still thinks we're logged in.
    if (response.status === 401 && currentUser) {
        resetToLogin("Your session has expired. Please log in again.");
    }

    if (response.status === 204) return null;

    const text = await response.text();
    let data = null;

    try {
        data = text ? JSON.parse(text) : null;
    } catch {
        data = text;
    }

    if (!response.ok) {
        throw new Error(
            data?.message || data?.detail || `Request failed (${response.status})`
        );
    }

    return data;
}

function showMessage(id, message, success = false) {
    $(id).textContent = message;
    $(id).classList.toggle("success", success);
}

function setLoggedIn(loggedIn) {
    $("loginSection").hidden = loggedIn;
    $("dashboard").hidden = !loggedIn;
    $("logoutBtn").hidden = !loggedIn;
}

$("loginForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    showMessage("loginMessage", "");

    try {
        currentUser = await api("/api/auth/login", {
            method: "POST",
            body: JSON.stringify({
                email: $("email").value.trim(),
                password: $("password").value
            })
        });

        if (!currentUser?.role || !currentUser?.email) {
            throw new Error("Login response is missing user information.");
        }

        sessionStorage.setItem("user", JSON.stringify(currentUser));
        await showDashboard();
    } catch (error) {
        currentUser = null;
        setLoggedIn(false);
        showMessage("loginMessage", error.message);
    }
});

$("logoutBtn").addEventListener("click", async () => {
    try {
        await api("/api/auth/logout", { method: "POST" });
    } catch (error) {
        console.error("Logout request failed:", error);
    } finally {
        resetToLogin("You have logged out.", true);
    }
});

async function showDashboard() {
    $("welcomeTitle").textContent = `Welcome, ${currentUser.email}`;
    $("userInfo").textContent = `Role: ${currentUser.role}`;
    setLoggedIn(true);

    if (currentUser.role === "ADMIN") {
        $("dashboardNav").hidden = false;
        await loadList("students");
    } else {
        $("dashboardNav").hidden = true;
        $("managementSection").hidden = true;
        $("profileSection").hidden = false;

        currentType = currentUser.role.toLowerCase() + "s";
        await loadOwnProfile();
    }
}

function resetToLogin(message, success = false) {
    currentUser = null;
    currentProfile = null;
    profiles = [];
    sessionStorage.removeItem("user");
    $("loginForm").reset();
    $("profileForm").reset();
    setLoggedIn(false);
    showMessage("loginMessage", message, success);
}

$("dashboardNav").addEventListener("click", async (event) => {
    const type = event.target.dataset.type;
    if (!type) return;

    await loadList(type);
});

async function loadList(type) {
    currentType = type;
    $("managementSection").hidden = false;
    $("profileSection").hidden = true;
    $("listTitle").textContent =
        type === "students" ? "Manage Students" : "Manage Teachers";

    $("tableHead").innerHTML = `
        <tr>
            <th>ID</th><th>Name</th>
            <th>Email</th><th>Department</th><th>Actions</th>
        </tr>`;

    try {
        profiles = await api(`/api/${type}`);
        renderTable();
        showMessage("managementMessage", "");
    } catch (error) {
        showMessage("managementMessage", error.message);
    }
}

function renderTable() {
    $("tableBody").replaceChildren();

    profiles.forEach((profile) => {
        const row = document.createElement("tr");

        [
            profile.id,
            profile.name,
            profile.email,
            profile.department
        ].forEach((value) => {
            const cell = document.createElement("td");
            cell.textContent = value ?? "";
            row.appendChild(cell);
        });

        const actions = document.createElement("td");
        actions.className = "actions";

        const edit = document.createElement("button");
        edit.textContent = "Edit";
        edit.addEventListener("click", () => openEdit(profile));

        const remove = document.createElement("button");
        remove.textContent = "Delete";
        remove.className = "danger";
        remove.addEventListener("click", () => deleteProfile(profile));

        actions.append(edit, remove);
        row.appendChild(actions);
        $("tableBody").appendChild(row);
    });
}

// Hidden fields are disabled so they are not submitted.
function showField(id, show) {
    $(id).hidden = !show;
    $(id).querySelector("input").disabled = !show;
}

// Sets the heading, the button label (create vs edit) and which fields are shown:
// email is admin-only, batch is student-only, designation is teacher-only.
function setupProfileForm() {
    const isStudent = currentType === "students";
    const isAdmin = currentUser.role === "ADMIN";

    $("profileTitle").textContent = isAdmin
        ? (isStudent ? "Student Profile" : "Teacher Profile")
        : "My Profile";
    $("saveBtn").textContent = currentProfile ? "Save Profile" : "Create";
    showField("emailField", isAdmin);
    showField("batchField", isStudent);
    showField("designationField", !isStudent);
}

function openEdit(profile) {
    currentProfile = profile;
    $("profileSection").hidden = false;
    $("managementSection").hidden = true;
    setupProfileForm();

    const form = $("profileForm");
    form.elements.email.value = profile.email ?? "";
    form.elements.password.value = "";
    form.elements.name.value = profile.name ?? "";
    form.elements.phoneNumber.value = profile.phoneNumber ?? "";
    form.elements.department.value = profile.department ?? "";
    form.elements.batch.value = profile.batch ?? "";
    form.elements.designation.value = profile.designation ?? "";

    showMessage("profileMessage", "");
}

$("addBtn").addEventListener("click", () => {
    currentProfile = null;
    $("profileSection").hidden = false;
    $("managementSection").hidden = true;
    setupProfileForm();
    $("profileForm").reset();
    showMessage("profileMessage", "");
});

$("profileForm").addEventListener("submit", async (event) => {
    event.preventDefault();

    const form = $("profileForm");
    const values = Object.fromEntries(new FormData(form).entries());

    if (!values.password) delete values.password;

    try {
        if (currentUser.role === "ADMIN") {
            const url = currentProfile
                ? `/api/${currentType}/${currentProfile.id}`
                : `/api/${currentType}`;

            const saved = await api(url, {
                method: currentProfile ? "PUT" : "POST",
                body: JSON.stringify(values)
            });

            if (currentProfile) {
                showMessage("profileMessage", "Profile updated.", true);
            } else {
                showMessage("profileMessage", "Profile created.", true);
            }

            await loadList(currentType);
        } else {
            const saved = await api(
                `/api/${currentType}/${currentProfile.id}`,
                {
                    method: "PUT",
                    body: JSON.stringify(values)
                }
            );

            currentProfile = saved;
            showMessage("profileMessage", "Profile updated.", true);
        }
    } catch (error) {
        showMessage("profileMessage", error.message);
    }
});

async function deleteProfile(profile) {
    if (!confirm(`Delete ${profile.name}? This cannot be undone.`)) return;

    try {
        await api(`/api/${currentType}/${profile.id}`, {
            method: "DELETE"
        });

        showMessage("managementMessage", "Profile deleted.", true);
        await loadList(currentType);
    } catch (error) {
        showMessage("managementMessage", error.message);
    }
}

async function loadOwnProfile() {
    try {
        currentProfile = await api(
            `/api/${currentType}/${currentUser.profileId}`
        );
        openEdit(currentProfile);
        $("profileSection").hidden = false;
        $("managementSection").hidden = true;
    } catch (error) {
        showMessage("profileMessage", error.message);
    }
}

// Restore the login after a page refresh; the server session cookie survives it.
currentUser = JSON.parse(sessionStorage.getItem("user"));
if (currentUser) showDashboard();
