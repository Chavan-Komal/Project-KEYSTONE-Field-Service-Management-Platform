import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Layout } from './components/Layout';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Home } from './pages/Home';
import { DispatcherBoard } from './pages/DispatcherBoard';
import { WorkOrderDetail } from './pages/WorkOrderDetail';
import { TechnicianView } from './pages/TechnicianView';
import { ManagerDashboard } from './pages/ManagerDashboard';
import { CustomerPortal } from './pages/CustomerPortal';
import { NewWorkOrder } from './pages/NewWorkOrder';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

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
