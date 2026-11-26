<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="../common/header.jsp">
    <jsp:param name="pageTitle" value="Advice & AI Chat - Finance."/>
</jsp:include>

<jsp:include page="../common/navbar.jsp">
    <jsp:param name="role" value="USER"/>
</jsp:include>

<div class="app-container">
    <jsp:include page="../common/sidebar.jsp">
        <jsp:param name="activePage" value="advice"/>
    </jsp:include>

    <main class="main-content">
        <div class="content-wrapper">

            <c:if test="${not empty successMessage}">
                <div class="alert alert-success mb-4">${successMessage}</div>
            </c:if>
            <c:if test="${not empty errorMessage}">
                <div class="alert alert-error mb-4">${errorMessage}</div>
            </c:if>

            <div class="page-header">
                <div>
                    <h1 class="page-title">Advice &amp; AI Chat</h1>
                    <p class="page-subtitle">Get instant guidance from the AI Financial Assistant or send an advice request to a human advisor.</p>
                </div>
            </div>

            <div class="dashboard-grid mb-4">

                <!-- AI Financial Assistant Chat -->
                <div class="card">
                    <div class="card-header">
                        <div class="card-title">AI Financial Assistant</div>
                        <span class="badge badge-success">Instant</span>
                    </div>

                    <div class="chat-window" id="aiChatWindow">
                        <c:forEach items="${chatHistory}" var="msg">
                            <div class="chat-bubble ${msg.sender == 'user' ? 'user' : 'bot'}">
                                <c:if test="${msg.sender != 'user'}">
                                    <div class="chat-meta">AI Assistant (${msg.mode == 'gemini' ? 'Gemini' : 'Smart Analyzer'})</div>
                                </c:if>
                                <c:out value="${msg.text}"/>
                            </div>
                        </c:forEach>
                    </div>

                    <div class="chat-prompts">
                        <button type="button" class="chat-chip" data-prompt="Analyze my current spending">Analyze my spending</button>
                        <button type="button" class="chat-chip" data-prompt="Am I over budget anywhere?">Check budget health</button>
                        <button type="button" class="chat-chip" data-prompt="How can I save more money?">How can I save more?</button>
                    </div>

                    <form id="aiChatForm" method="post" action="${pageContext.request.contextPath}/user/advice/ask-ai" class="chat-input-row">
                        <input type="hidden" name="_csrf" id="aiCsrfToken" value="${sessionScope.csrfToken}">
                        <input type="text" id="aiPromptInput" name="prompt" class="form-control"
                               placeholder="Ask about your expenses, budgets, or savings..." maxlength="300" required>
                        <button type="submit" id="aiSendBtn" class="btn btn-primary">Ask AI</button>
                    </form>
                </div>

                <!-- Request Advice from Human Advisor -->
                <div class="card">
                    <div class="card-header">
                        <div class="card-title">Request Human Advisor</div>
                        <span class="badge">Personal Review</span>
                    </div>

                    <form method="post" action="${pageContext.request.contextPath}/user/advice/request-advisor">
                        <input type="hidden" name="_csrf" value="${sessionScope.csrfToken}">
                        <div class="form-group">
                            <label for="advisor_id" class="form-label">Select Advisor (Optional)</label>
                            <select id="advisor_id" name="advisor_id" class="form-control">
                                <option value="">Any Available Advisor</option>
                                <c:forEach items="${advisors}" var="adv">
                                    <option value="${adv.id}">${adv.name} (${adv.email})</option>
                                </c:forEach>
                            </select>
                        </div>

                        <div class="form-group">
                            <label for="subject" class="form-label">Topic / Subject</label>
                            <input type="text" id="subject" name="subject" class="form-control"
                                   placeholder="e.g., Monthly budget review, Debt reduction..." maxlength="80" required>
                        </div>

                        <div class="form-group">
                            <label for="request_message" class="form-label">Your Question or Request</label>
                            <textarea id="request_message" name="message" class="form-control"
                                      placeholder="Describe what financial advice you need from an advisor..." maxlength="300" required></textarea>
                            <span class="form-hint">Maximum 300 characters</span>
                        </div>

                        <button type="submit" class="btn btn-primary btn-block mt-2">Send Request to Advisor</button>
                    </form>
                </div>

            </div>

            <!-- User's Submitted Advisor Requests & Received Advice -->
            <div class="dashboard-grid">
                <div class="card">
                    <div class="card-header">
                        <div class="card-title">Your Advisor Requests</div>
                    </div>

                    <c:choose>
                        <c:when test="${not empty myRequests}">
                            <div class="table-container">
                                <table class="table">
                                    <thead>
                                        <tr>
                                            <th>Date</th>
                                            <th>Advisor</th>
                                            <th>Topic &amp; Message</th>
                                            <th class="text-right">Status</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach items="${myRequests}" var="req">
                                            <tr>
                                                <td class="text-muted">${req.createdAt}</td>
                                                <td>
                                                    <c:choose>
                                                        <c:when test="${not empty req.advisorName}">${req.advisorName}</c:when>
                                                        <c:otherwise><span class="text-muted">Any Advisor</span></c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td>
                                                    <strong><c:out value="${req.subject}"/></strong><br>
                                                    <span class="text-secondary"><c:out value="${req.message}"/></span>
                                                </td>
                                                <td class="text-right">
                                                    <span class="badge ${req.status == 'ANSWERED' ? 'badge-success' : ''}">${req.status}</span>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="empty-state">
                                <div class="empty-state-title">No advisor requests yet</div>
                                <div class="empty-state-text">Use the form above whenever you want a human advisor to review your spending.</div>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>

                <div class="card">
                    <div class="card-header">
                        <div class="card-title">Advice Received from Advisors</div>
                    </div>

                    <c:choose>
                        <c:when test="${not empty receivedAdvice}">
                            <div class="table-container">
                                <table class="table">
                                    <thead>
                                        <tr>
                                            <th>Date</th>
                                            <th>Advisor ID</th>
                                            <th>Advice</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach items="${receivedAdvice}" var="adv">
                                            <tr>
                                                <td class="text-muted">${adv.date}</td>
                                                <td class="font-mono">${adv.advisorId}</td>
                                                <td><c:out value="${adv.message}"/></td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="empty-state">
                                <div class="empty-state-title">No advisor replies yet</div>
                                <div class="empty-state-text">Replies from advisors will appear here once they respond to your request.</div>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

        </div>

<script>
(function () {
    const form = document.getElementById('aiChatForm');
    const input = document.getElementById('aiPromptInput');
    const sendBtn = document.getElementById('aiSendBtn');
    const chatWindow = document.getElementById('aiChatWindow');

    function appendBubble(sender, text, mode) {
        const bubble = document.createElement('div');
        bubble.className = 'chat-bubble ' + sender;
        if (sender === 'bot') {
            const meta = document.createElement('div');
            meta.className = 'chat-meta';
            meta.textContent = 'AI Assistant (' + (mode === 'gemini' ? 'Gemini' : 'Smart Analyzer') + ')';
            bubble.appendChild(meta);
        }
        bubble.appendChild(document.createTextNode(text));
        chatWindow.appendChild(bubble);
        chatWindow.scrollTop = chatWindow.scrollHeight;
    }

    async function submitPrompt(promptText) {
        const trimmed = promptText.trim();
        if (!trimmed) return;

        appendBubble('user', trimmed);
        input.value = '';
        sendBtn.disabled = true;
        sendBtn.textContent = 'Thinking...';

        try {
            const csrfInput = document.getElementById('aiCsrfToken');
            const body = new URLSearchParams({
                prompt: trimmed,
                _csrf: csrfInput ? csrfInput.value : ''
            });
            const res = await fetch(form.action, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
                    'X-Requested-With': 'fetch'
                },
                body: body.toString()
            });
            const data = await res.json();
            if (data.reply) {
                appendBubble('bot', data.reply, data.mode);
            } else {
                appendBubble('bot', data.error || 'Could not generate advice right now.', 'local');
            }
        } catch (err) {
            appendBubble('bot', 'Network error while contacting AI Assistant.', 'local');
        } finally {
            sendBtn.disabled = false;
            sendBtn.textContent = 'Ask AI';
            input.focus();
        }
    }

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        submitPrompt(input.value);
    });

    document.querySelectorAll('.chat-chip').forEach(function (btn) {
        btn.addEventListener('click', function () {
            submitPrompt(btn.getAttribute('data-prompt') || '');
        });
    });

    chatWindow.scrollTop = chatWindow.scrollHeight;
})();
</script>
<jsp:include page="../common/footer.jsp"/>
