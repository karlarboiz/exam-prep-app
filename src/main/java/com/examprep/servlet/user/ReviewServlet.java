package com.examprep.servlet.user;

import com.examprep.model.StudyPlan;
import com.examprep.model.User;
import com.examprep.service.WeeklyRegimenService;
import com.examprep.util.IdCipher;
import com.examprep.util.WebUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/user/review")
public class ReviewServlet extends HttpServlet {

    private final WeeklyRegimenService weeklyRegimenService = new WeeklyRegimenService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = WebUtil.getCurrentUser(req);
        try {
            Long regimenId = null;
            String regimenParam = req.getParameter("regimenId");
            if (regimenParam != null && !regimenParam.isBlank()) {
                regimenId = IdCipher.dec(regimenParam);
            }
            StudyPlan plan = weeklyRegimenService.getStudyPlan(user.getId(), regimenId);
            req.setAttribute("studyPlan", plan);
            req.setAttribute("misses", plan.getMisses());
            req.setAttribute("regimen", plan.getRegimen());
            req.getRequestDispatcher("/WEB-INF/jsp/user/review.jsp").forward(req, resp);
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
