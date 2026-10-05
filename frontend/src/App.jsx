import { Navigate, Route, Routes } from 'react-router-dom'
import { auth } from './api/client'
import Layout from './components/Layout.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Users from './pages/Users.jsx'
import UserForm from './pages/UserForm.jsx'
import UserReport from './pages/UserReport.jsx'
import UserPage from './pages/UserPage.jsx'
import Branches from './pages/Branches.jsx'
import BranchForm from './pages/BranchForm.jsx'
import GroupForm from './pages/GroupForm.jsx'
import UserImport from './pages/UserImport.jsx'
import Courses from './pages/Courses.jsx'
import CourseForm from './pages/CourseForm.jsx'
import CourseDetails from './pages/CourseDetails.jsx'
import CourseReport from './pages/CourseReport.jsx'
import Categories from './pages/Categories.jsx'
import Notifications from './pages/Notifications.jsx'
import NotificationForm from './pages/NotificationForm.jsx'
import Timeline from './pages/Timeline.jsx'
import Reports from './pages/Reports.jsx'
import MyCourses from './pages/MyCourses.jsx'
import Catalog from './pages/Catalog.jsx'
import MyProgress from './pages/MyProgress.jsx'
import UnitForm from './pages/UnitForm.jsx'
import UnitView from './pages/UnitView.jsx'
import Settings from './pages/Settings.jsx'
import ChangePassword from './pages/ChangePassword.jsx'
import CertificateView from './pages/CertificateView.jsx'
import TestForm, { NewTest } from './pages/TestForm.jsx'
import QuestionForm from './pages/QuestionForm.jsx'

function Private({ children }) {
  return auth.token() ? children : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/change-password" element={<Private><ChangePassword /></Private>} />
      <Route path="/" element={<Private><Layout /></Private>}>
        <Route index element={<Dashboard />} />
        <Route path="users" element={<Users />} />
        <Route path="users/new" element={<UserForm />} />
        <Route path="users/import" element={<UserImport />} />
        <Route path="users/:id/report" element={<UserReport view="progress" />} />
        <Route path="users/:id/infographic" element={<UserReport view="infographic" />} />
        <Route path="users/:id/edit" element={<UserPage tab="info" />} />
        <Route path="users/:id/courses" element={<UserPage tab="courses" />} />
        <Route path="users/:id/groups" element={<UserPage tab="groups" />} />
        <Route path="users/:id/branches" element={<UserPage tab="branches" />} />
        <Route path="users/:id/files" element={<UserPage tab="files" />} />
        <Route path="branches" element={<Branches kind="branches" />} />
        <Route path="branches/new" element={<BranchForm />} />
        <Route path="branches/:id/edit" element={<BranchForm />} />
        <Route path="groups" element={<Branches kind="groups" />} />
        <Route path="groups/new" element={<GroupForm />} />
        <Route path="groups/:id/edit" element={<GroupForm />} />
        <Route path="courses" element={<Courses />} />
        <Route path="courses/new" element={<CourseForm />} />
        <Route path="courses/:id" element={<CourseDetails />} />
        <Route path="courses/:id/edit" element={<CourseForm tab="course" />} />
        <Route path="courses/:id/edit/users" element={<CourseForm tab="users" />} />
        <Route path="courses/:id/reports" element={<CourseReport tab="overview" />} />
        <Route path="courses/:id/reports/users" element={<CourseReport tab="users" />} />
        <Route path="courses/:id/reports/matrix" element={<CourseReport tab="matrix" />} />
        <Route path="courses/:id/reports/timeline" element={<CourseReport tab="timeline" />} />
        <Route path="courses/:courseId/units/new" element={<UnitForm />} />
        <Route path="units/:unitId" element={<UnitView />} />
        <Route path="units/:unitId/edit" element={<UnitForm />} />
        <Route path="courses/:courseId/tests/new" element={<NewTest />} />
        <Route path="units/:unitId/test/edit" element={<TestForm />} />
        <Route path="courses/:courseId/questions/new" element={<QuestionForm />} />
        <Route path="questions/:id/edit" element={<QuestionForm />} />
        <Route path="my-courses" element={<MyCourses />} />
        <Route path="catalog" element={<Catalog />} />
        <Route path="my-progress" element={<MyProgress />} />
        <Route path="categories" element={<Categories />} />
        <Route path="notifications" element={<Notifications tab="rules" />} />
        <Route path="notifications/history" element={<Notifications tab="history" />} />
        <Route path="notifications/pending" element={<Notifications tab="pending" />} />
        <Route path="notifications/new" element={<NotificationForm />} />
        <Route path="notifications/:id/edit" element={<NotificationForm />} />
        <Route path="reports" element={<Reports />} />
        <Route path="timeline" element={<Timeline />} />
        <Route path="settings" element={<Settings tab="basic" />} />
        <Route path="settings/users" element={<Settings tab="users" />} />
        <Route path="settings/certificates" element={<Settings tab="certificates" />} />
        <Route path="settings/gamification" element={<Settings tab="gamification" />} />
        <Route path="certificates/:id" element={<CertificateView />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
