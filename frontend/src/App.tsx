import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Layout } from './components/Layout';
import { Login } from './pages/Login';
import { DemoLogin } from './pages/DemoLogin';
import { Register } from './pages/Register';
import { ForgotPassword } from './pages/ForgotPassword';
import { ResetPassword } from './pages/ResetPassword';
import { Home } from './pages/Home';
import { DispatcherBoard } from './pages/DispatcherBoard';
import { WorkOrderDetail } from './pages/WorkOrderDetail';
import { TechnicianView } from './pages/TechnicianView';
import { ManagerDashboard } from './pages/ManagerDashboard';
import { CustomerPortal } from './pages/CustomerPortal';
import { NewWorkOrder } from './pages/NewWorkOrder';
import { ManagerTechnicians } from './pages/ManagerTechnicians';
import { TrackingMap } from './pages/TrackingMap';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/demo-login" element={<DemoLogin />} />
          <Route path="/register" element={<Register />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />

          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Layout>
                  <Home />
                </Layout>
              </ProtectedRoute>
            }
          />

          <Route
            path="/board"
            element={
              <ProtectedRoute allow={['DISPATCHER', 'MANAGER']}>
                <Layout>
                  <DispatcherBoard />
                </Layout>
              </ProtectedRoute>
            }
          />

          <Route
            path="/my-jobs"
            element={
              <ProtectedRoute allow={['TECHNICIAN']}>
                <Layout>
                  <TechnicianView />
                </Layout>
              </ProtectedRoute>
            }
          />

          <Route
            path="/dashboard"
            element={
              <ProtectedRoute allow={['MANAGER']}>
                <Layout>
                  <ManagerDashboard />
                </Layout>
              </ProtectedRoute>
            }
          />

          <Route
            path="/portal"
            element={
              <ProtectedRoute allow={['CUSTOMER']}>
                <Layout>
                  <CustomerPortal />
                </Layout>
              </ProtectedRoute>
            }
          />

          <Route
            path="/new-request"
            element={
              <ProtectedRoute>
                <Layout>
                  <NewWorkOrder />
                </Layout>
              </ProtectedRoute>
            }
          />

          <Route
            path="/technicians"
            element={
              <ProtectedRoute allow={['MANAGER']}>
                <Layout>
                  <ManagerTechnicians />
                </Layout>
              </ProtectedRoute>
            }
          />

          <Route
            path="/map"
            element={
              <ProtectedRoute allow={['MANAGER', 'CUSTOMER']}>
                <Layout>
                  <TrackingMap />
                </Layout>
              </ProtectedRoute>
            }
          />

          <Route
            path="/work-orders/:id"
            element={
              <ProtectedRoute>
                <Layout>
                  <WorkOrderDetail />
                </Layout>
              </ProtectedRoute>
            }
          />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
