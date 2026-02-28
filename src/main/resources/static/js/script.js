document.addEventListener('DOMContentLoaded', () => {
    const API_BASE_URL = '/api/auth';
    let currentUserEmail = '';
    let currentToken = '';

    // Element Selectors
    const views = document.querySelectorAll('.auth-view');
    const toast = document.getElementById('toast');
    const toastMsg = document.getElementById('toast-message');
    const cardDesc = document.getElementById('card-description');

    // Navigation Links
    const linkToSignup = document.getElementById('to-signup');
    const linkToLogin = document.getElementById('to-login');
    const linkToForgot = document.getElementById('to-forgot-password');
    const linkBackToLogin = document.getElementById('back-to-login');

    // Forms
    const loginForm = document.getElementById('login-view');
    const signupForm = document.getElementById('signup-view');
    const otpForm = document.getElementById('otp-view');
    const passwordForm = document.getElementById('password-view');
    const forgotForm = document.getElementById('forgot-password-view');

    // --- View Management ---

    function switchView(viewId) {
        views.forEach(v => v.classList.remove('active'));
        document.getElementById(viewId).classList.add('active');

        // Update header description based on view
        const descriptions = {
            'login-view': 'Welcome back! Please enter your details.',
            'signup-view': 'Join us today. Enter your email to start.',
            'otp-view': 'We\'ve sent a verification code to your email.',
            'password-view': 'Final step! Please secure your account.',
            'forgot-password-view': 'Don\'t worry, we\'ll help you reset it.'
        };
        cardDesc.textContent = descriptions[viewId] || '';
    }

    linkToSignup.onclick = (e) => { e.preventDefault(); switchView('signup-view'); };
    linkToLogin.onclick = (e) => { e.preventDefault(); switchView('login-view'); };
    linkToForgot.onclick = (e) => { e.preventDefault(); switchView('forgot-password-view'); };
    linkBackToLogin.onclick = (e) => { e.preventDefault(); switchView('login-view'); };

    // --- API Interactions ---

    async function apiCall(endpoint, method = 'POST', body = null) {
        const options = {
            method,
            headers: { 'Content-Type': 'application/json' }
        };
        if (body) options.body = JSON.stringify(body);

        try {
            const response = await fetch(`${API_BASE_URL}${endpoint}`, options);
            const result = await response.json();
            return result;
        } catch (error) {
            console.error('API Error:', error);
            return { statusCode: 500, statusMessage: 'Internal Server Error', data: error.message };
        }
    }

    function showToast(message, type = 'error') {
        toastMsg.textContent = message;
        toast.className = `toast ${type}`;
        toast.classList.remove('hidden');
        setTimeout(() => toast.classList.add('hidden'), 5000);
    }

    document.querySelector('.toast-close').onclick = () => toast.classList.add('hidden');

    function setBtnLoading(btnId, isLoading) {
        const btn = document.getElementById(btnId);
        if (isLoading) btn.classList.add('loading');
        else btn.classList.remove('loading');
        btn.disabled = isLoading;
    }

    // --- Feature Implementations ---

    // 1. Signup
    signupForm.onsubmit = async (e) => {
        e.preventDefault();
        const email = document.getElementById('signup-email').value;
        currentUserEmail = email;

        setBtnLoading('signup-btn', true);
        const res = await apiCall('/signup', 'POST', { email });
        setBtnLoading('signup-btn', false);

        if (res.statusCode === 200) {
            // Handle "Already registered" case (personalized message)
            if (res.data.includes('already registered')) {
                showToast(res.data, 'success');
                switchView('login-view');
            } else {
                document.getElementById('display-email').textContent = email;
                showToast('OTP sent successfully!', 'success');
                switchView('otp-view');
            }
        } else {
            showToast(res.data || res.statusMessage);
        }
    };

    // 2. OTP Verification
    const otpDigits = document.querySelectorAll('.otp-digit');
    otpDigits.forEach((digit, i) => {
        digit.onkeyup = (e) => {
            if (e.key >= 0 && e.key <= 9) otpDigits[i + 1]?.focus();
            if (e.key === 'Backspace') otpDigits[i - 1]?.focus();
        };
    });

    otpForm.onsubmit = async (e) => {
        e.preventDefault();
        const otp = Array.from(otpDigits).map(d => d.value).join('');
        if (otp.length < 6) return showToast('Please enter full 6-digit OTP');

        setBtnLoading('verify-otp-btn', true);
        const res = await apiCall('/verify-otp', 'POST', { email: currentUserEmail, otp });
        setBtnLoading('verify-otp-btn', false);

        if (res.statusCode === 200) {
            handleAuthSuccess(res.data);
        } else {
            showToast(res.data || res.statusMessage);
        }
    };

    // 3. Login
    loginForm.onsubmit = async (e) => {
        e.preventDefault();
        const email = document.getElementById('login-email').value;
        const password = document.getElementById('login-password').value;
        currentUserEmail = email;

        setBtnLoading('login-btn', true);
        const res = await apiCall('/login', 'POST', { email, password });
        setBtnLoading('login-btn', false);

        if (res.statusCode === 200) {
            handleAuthSuccess(res.data);
        } else {
            showToast(res.data || res.statusMessage);
        }
    };

    function handleAuthSuccess(authData) {
        currentToken = authData.token;
        const user = authData.userDTO;

        // Logic for isPasswordSet logic
        if (user.passwordSet) {
            showToast(`Welcome back, ${user.firstName || 'User'}!`, 'success');
            // Redirect or update UI for logged-in state
            console.log('User logged in:', user);
        } else {
            showToast('Please set a password for your new account.', 'success');
            switchView('password-view');
        }
    }

    // 4. Set/Change Password
    passwordForm.onsubmit = async (e) => {
        e.preventDefault();
        const newPassword = document.getElementById('new-password').value;
        const confirmPassword = document.getElementById('confirm-password').value;

        if (newPassword !== confirmPassword) return showToast('Passwords do not match');

        setBtnLoading('set-password-btn', true);
        // Using resetPassword endpoint since it handles the same logic for new users
        const res = await apiCall('/reset-password', 'POST', {
            token: 'TOKEN_HERE', // In a real flow, this would be tied to session or temp token
            newPassword,
            confirmPassword
        });
        setBtnLoading('set-password-btn', false);

        if (res.statusCode === 200) {
            showToast('Password set successfully! Log in now.', 'success');
            switchView('login-view');
        } else {
            showToast(res.data || res.statusMessage);
        }
    };

    // 5. Forgot Password
    forgotForm.onsubmit = async (e) => {
        e.preventDefault();
        const email = document.getElementById('forgot-email').value;

        setBtnLoading('forgot-btn', true);
        const res = await apiCall('/forgot-password', 'POST', { email });
        setBtnLoading('forgot-btn', false);

        if (res.statusCode === 200) {
            showToast('Reset link sent to your email!', 'success');
            switchView('login-view');
        } else {
            showToast(res.data || res.statusMessage);
        }
    };

    // --- Google SSO Implementation ---
    let client;
    const googleClientId = '685512374164-k22f1kn6shn3vu2ll527elret13lc50t.apps.googleusercontent.com';

    window.onload = function () {
        client = google.accounts.oauth2.initCodeClient({
            client_id: googleClientId,
            scope: 'openid email profile',
            ux_mode: 'popup',
            callback: (response) => {
                if (response.code) {
                    handleGoogleCode(response.code);
                }
            },
        });

        document.getElementById('google-login-btn').onclick = () => {
            client.requestCode();
        };
    };

    async function handleGoogleCode(code) {
        showToast('Verifying Google account...', 'success');

        const res = await apiCall(`/google-login?code=${encodeURIComponent(code)}`, 'POST');
        if (res.statusCode === 200) {
            handleAuthSuccess(res.data);
        } else {
            showToast(res.data || res.statusMessage);
        }
    }
});
